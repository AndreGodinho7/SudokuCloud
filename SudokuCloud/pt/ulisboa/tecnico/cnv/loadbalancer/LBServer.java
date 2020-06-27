package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import pt.ulisboa.tecnico.cnv.exceptions.*;
import pt.ulisboa.tecnico.cnv.requestinfo.*;
import pt.ulisboa.tecnico.cnv.timertasks.*;
import pt.ulisboa.tecnico.cnv.timertasks.CheckinstancesTask;
//import pt.ulisboa.tecnico.cnv.timertasks.QueryDynamoTask;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.Executors;

public class LBServer {
    private final static int port = 8000;
    private final static String endpoint = "/sudoku";
    private static final String protocol = "http";
    private static final String agent = "Sudoku-LB";
    private static final int HEALTH_CHECK_PERIOD = 20 * 1000;
    private static final int HEALTH_CHECK_DELAY = 0 * 1000;

    private static final int AUTOSCALING_CHECK_PERIOD = 15 * 1000;
    private static final int AUTOSCALING_CHECK_DELAY = 0 * 1000;

    private static final int CHECK_INSTANCES_PERIOD = 40 * 1000;
    private static final int CHECK_INSTANCES_DELAY = 0 * 1000;

    private static final int WRITE_DYNAMO_PEIOD = 2 * 60 * 1000;
    private static final int WRITE_DYNAMO_DELAY = 0 * 1000;

    private static final double BIG_REQUEST_THRESHOLD = 0.5;
    private static final double SMALL_REQUEST_THRESHOLD = 0.15;

    private static final int BIG_REQUEST_FREQUENCY = 1000; // in milliseconds
    private static final int INTERMEDIATE_REQUEST_FREQUENCY = 2000; // in milliseconds
    private static final int SMALL_REQUEST_FREQUENCY = 1000; // in milliseconds
    private static final int RESERVED_REQUEST_FREQUENCY = 1000;

    private static final int TIMEOUT_CONNECTION = 10 * 60 * 1000;

    private static InstanceManager instanceManager = InstanceManager.getInstance();
    private static CostPredictor predictor = CostPredictor.getCostPredictor();

    private static final double OVERTAKEN_COEFFICIENT = 0.2;
    private static BigRequestsManager overtakenRequests = BigRequestsManager.getBigRequestsManager();

    public static void main(final String[] args) throws Exception {

        List<Map<String, AttributeValue>> table;
        table = instanceManager.queryDynamoDB();

        if (table.size() > 0) {
            System.out.println("INIT: DynamoDB database is not empty. Querying entire database to cache.");
            predictor.initCache(table);
        } else System.out.println("INIT: DynamoDB database is empty.");

        Timer healthChecks = new Timer();
        healthChecks.schedule(new HealthChecksTask(), HEALTH_CHECK_DELAY, HEALTH_CHECK_PERIOD);

        Timer autoScale = new Timer();
        autoScale.schedule(new AutoScallingTask(), AUTOSCALING_CHECK_DELAY, AUTOSCALING_CHECK_PERIOD);

        Timer checkInstances = new Timer();
        checkInstances.schedule(new CheckinstancesTask(), CHECK_INSTANCES_DELAY, CHECK_INSTANCES_PERIOD);

        Timer writeDynamo = new Timer();
        writeDynamo.schedule(new WriteToDynamoTask(), WRITE_DYNAMO_DELAY, WRITE_DYNAMO_PEIOD);

        final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext(endpoint, new LBHandler());

        // be aware! infinite pool of threads!
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Load balancer server initialized.");
        System.out.println(server.getAddress().toString());
    }

    protected static String parseRequestBody(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is, "utf-8");
        BufferedReader br = new BufferedReader(isr);

        // From now on, the right way of moving from bytes to utf-8 characters:
        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);

        }

        br.close();
        isr.close();

        return buf.toString();
    }

    private static String buildURL(MyInstance instance, String query, String puzzle_array) {
        String dns = instance.getIp();
        int port = instance.getPort();

        if (query == null) {
            return String.format("%s://%s:%d%s", protocol, dns, port, endpoint);
        }

        return String.format("%s://%s:%d%s?%s&b=%s", protocol, dns, port, endpoint, query, puzzle_array);
    }

    static class LBHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange t) throws IOException {
            // Get the query.
            final String query = t.getRequestURI().getQuery();
            System.out.println("> Query:\t" + query);

            // Break it down into String[].
            final String[] params = query.split("&");

            // Store as if it was a direct call to SolverMain.
            final ArrayList<String> newArgs = new ArrayList<>();
            final Dictionary request_parameters = new Hashtable();
            for (final String p : params) {
                final String[] splitParam = p.split("=");
                request_parameters.put(splitParam[0], splitParam[1]);
                newArgs.add("-" + splitParam[0]);
                newArgs.add(splitParam[1]);
            }
            newArgs.add("-b");
            newArgs.add(parseRequestBody(t.getRequestBody()));

            String puzzle_array = newArgs.get(newArgs.size() - 1);

            newArgs.add("-d");

            // Store from ArrayList into regular String[].
            final String[] args = new String[newArgs.size()];
            int i = 0;
            for (String arg : newArgs) {
                args[i] = arg;
                i++;
            }

            // Auxiliar variables
            String puzzle_name = request_parameters.get("i").toString();
            String strategy = request_parameters.get("s").toString();
            int n1 = Integer.parseInt(request_parameters.get("n1").toString());
            int n2 = Integer.parseInt(request_parameters.get("n2").toString());
            int miss_ele = Integer.parseInt(request_parameters.get("un").toString());

            Request request;
            if (strategy.equals("BFS")) {
                request = new RequestBFS(puzzle_name, strategy, n1, n2, miss_ele);
            } else if (strategy.equals("CP")) {
                request = new RequestCP(puzzle_name, strategy, n1, n2, miss_ele);
            } else {
                request = new RequestDLX(puzzle_name, strategy, n1, n2, miss_ele);
            }

            // Load ballance
            double request_cost = predictor.predictCost(request);
            System.out.println("LOAD BALANCER MESSAGE: Predicted cost of the request " + request.toString() + ": " + (int) request_cost);
            double relative_predicted_cost = request_cost / InstanceManager.getMaximumCost();
            request.setPredicted_cost(request_cost);

            int checkLeastLoadedInstanceFrequency = 0;
            if (relative_predicted_cost >= BIG_REQUEST_THRESHOLD) {
                checkLeastLoadedInstanceFrequency = BIG_REQUEST_FREQUENCY;
                request.setRequest_type("BIG");
                request.setMax_overtaken_value(OVERTAKEN_COEFFICIENT); // predicted cost is already defined inside object
                overtakenRequests.insertBigRequest(Thread.currentThread().getId(), request);
            } else if ((relative_predicted_cost < BIG_REQUEST_THRESHOLD) && (relative_predicted_cost >= SMALL_REQUEST_THRESHOLD)) {
                checkLeastLoadedInstanceFrequency = INTERMEDIATE_REQUEST_FREQUENCY;
                request.setRequest_type("INTERMEDIATE");
            } else if (relative_predicted_cost < SMALL_REQUEST_THRESHOLD) {
                checkLeastLoadedInstanceFrequency = SMALL_REQUEST_FREQUENCY;
                request.setRequest_type("SMALL");
            }

            MyInstance instance = null;
            CloseableHttpResponse httpresponse = null;

            while (true) { // for fault tolerance
                while (true) {
                    try {
                        if ((request.getRequest_type().equals("BIG")) &&
                                (request.maxOverTakenBreached())) {
                            System.out.println("WARNING: Request has been overtaken multiple times. An instance will be reserved " +
                                    " for " + request.toString());
                            instanceManager.leastLoadedInstance(request_cost, true, request);
                        } else {
                            if ((!request.getRequest_type().equals("SMALL")) ||
                                    ((request.getRequest_type()).equals("SMALL ")) && (overtakenRequests.isBigRequestWaiting())) {
                                Thread.sleep(checkLeastLoadedInstanceFrequency);
                            }
                            instanceManager.leastLoadedInstance(request_cost, false, request);
                        }
                    } catch (NoInstanceAvailableException e) {
                        e.printStackTrace();
                        return;
                    } catch (ServerFullLoadedException e) {
                    } catch (SendSmallRequestException e) {
                        instance = e.getLeastLoadedInstance();
                        overtakenRequests.updateOvertakingRequests(request.getRequest_type(), request.getPredicted_cost());
                        break;
                    } catch (SendBigRequestException e) {
                        instance = e.getLeastLoadedInstance();
                        overtakenRequests.removeBigRequest(Thread.currentThread().getId());
                        break;
                    } catch (SendIntermediateRequestException e) {
                        instance = e.getLeastLoadedInstance();
                        overtakenRequests.updateOvertakingRequests(request.getRequest_type(), request.getPredicted_cost());
                        break;
                    } catch (SendBigRequestToReservedException e) {
                        instance = e.getLeastLoadedInstance();

                        while (true) {
                            try {
                                System.out.println(String.format("LOAD BALANCER WARNING: Instance DNS %s is reserved for request: ", instance.getIp()) + request.toString());
                                if (!instance.isHealthy()) {
                                    System.out.println("LOAD BALANCER ERROR: RESERVED INSTANCE IS UNHEALTHY.");
                                    if (!instanceManager.isInstanceinMap(instance.getId())){
                                        System.out.println("LOAD BALANCER ERROR: RESERVED INSTANCE BECAME UNHEALTHY.");
                                        break;
                                    }
                                    Thread.sleep(30000); // for health checks
                                    continue;
                                }
                                if (instance.getRelative_workload() < InstanceManager.getMaximumLoad()) {
                                    System.out.println("LOAD BALANCER WARNING: Instance is available for reserved request.");
                                    break;
                                }
                                Thread.sleep(RESERVED_REQUEST_FREQUENCY);
                            } catch (InterruptedException ex) {
                                System.out.println("Thread was interrupted when waiting for reservation.");
                                ex.printStackTrace();
                            } catch (Exception exc) {
                                exc.getStackTrace();
                            }
                        }
                        break;

                    } catch (InterruptedException e) {
                        System.out.println("Thread was interrupted.");
                        e.printStackTrace();
                    }
                }

                if (!instance.isReserved()) { // the reserved were added a new request
                    instance.addNewRequest();
                }

                System.out.println("LOAD BALANCER MESSAGE: IP of chosen instance: " + instance.getIp());
                System.out.println("LOAD BALANCER MESSAGE: Workload of the instance: " + instance.getRelative_workload());

                // send GET request to instance
//                CloseableHttpClient client = HttpClients.createDefault();
//                RequestConfig requestConfig = RequestConfig.custom().build();
                CloseableHttpClient client = HttpClients.custom()
                        .disableAutomaticRetries()
                        .build();

                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_CONNECTION)
                        .setConnectionRequestTimeout(TIMEOUT_CONNECTION)
                        .setSocketTimeout(TIMEOUT_CONNECTION).build();

                String url = buildURL(instance, query, puzzle_array);

                HttpGet httpget = new HttpGet(url);
                httpget.addHeader("User-Agent", agent);
                httpget.setConfig(requestConfig);
                // TODO: extender timeout do http request

                System.out.println("LOAD BALANCER MESSAGE: REQUEST SENT to: " + url);
                try {
                    if (instance.isReserved()) {
                        System.out.println(String.format("LOAD BALANCER WARNING: Instance with DNS %s will be unreserved.",
                                instance.getIp()));
                        instance.setReserved(false);
                    }

                    httpresponse = client.execute(httpget);
                    instance.finishRequest();
                    instance.setWorkload(instance.getWorkload() - request_cost);
                    instance.setRelative_workload(instance.getRelative_workload() - relative_predicted_cost);

                    break;

                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println(String.format("ERROR: Instance %s with DNS %s is unreachable.",
                            instance.getId(), instance.getIp()));
                    e.printStackTrace();
                    continue;
                }
            }

            int statuscode = httpresponse.getStatusLine().getStatusCode();

            System.out.println("GET response status: " + statuscode);

            HttpEntity entity = httpresponse.getEntity();
            String[] puzzle_cost = null;
            String responseString = null;

            // Send response to browser.
            if (statuscode == 200) {

                try {
                    responseString = EntityUtils.toString(entity, "UTF-8");
                    System.out.println(responseString);

                    puzzle_cost = responseString.split("--");

                    double true_cost = Double.parseDouble(puzzle_cost[1]);
                    System.out.println(String.format("LOAD BALANCER MESSAGE: %f - COST of request: ", true_cost) + request.toString());

                    if (true_cost > InstanceManager.getMaximumCost()) {
                        System.out.println("LOAD BALANCER WARNING: A new maximum cost was received: " + true_cost);
                        InstanceManager.setMaximumCost(true_cost);
                    }

                    Measurement newCachedMeasurement = new Measurement(request, true_cost);
                    try {
                        predictor.insertInLBCache(newCachedMeasurement);
                        predictor.printCache();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                final Headers hdrs = t.getResponseHeaders();

                hdrs.add("Content-Type", "application/json");

                hdrs.add("Access-Control-Allow-Origin", "*");

                hdrs.add("Access-Control-Allow-Credentials", "true");
                hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
                hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

                t.sendResponseHeaders(200, puzzle_cost[0].length()); // responseString

                final OutputStream os = t.getResponseBody();
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                osw.write(puzzle_cost[0]); // responseString
                osw.flush();
                osw.close();

                os.close();

                System.out.println("> Sent response to " + t.getRemoteAddress().toString());
            } else {
                t.sendResponseHeaders(statuscode, responseString.length());
                OutputStream os = t.getResponseBody();
                os.write(responseString.getBytes());
                os.close();
            }
        }
    }
}