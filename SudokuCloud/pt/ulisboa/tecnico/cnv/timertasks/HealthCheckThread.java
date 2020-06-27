package pt.ulisboa.tecnico.cnv.timertasks;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import pt.ulisboa.tecnico.cnv.loadbalancer.MyInstance;

import java.io.IOException;

public class HealthCheckThread extends Thread {
    private static final String protocol = "http";
    private static final String agent = "Sudoku-LB";
    private final static String endpoint = "/health";
    private static final int timeout = 5*1000;
    private MyInstance myInstance;

    public HealthCheckThread(MyInstance instance) {
        this.myInstance = instance;
    }

    private static String buildURL(MyInstance instance) {
        String dns = instance.getIp();
        int port = instance.getPort();

        return String.format("%s://%s:%d%s", protocol, dns, port, endpoint);
    }

    public void run(){
        System.out.println(String.format("Sending healthcheck to instance %s with DNS %s",
                                        this.myInstance.getId(), this.myInstance.getIp()));

        RequestConfig config_timeouts = RequestConfig.custom()
                .setConnectTimeout(this.timeout * 1000)
                .setConnectionRequestTimeout(this.timeout * 1000)
                .setSocketTimeout(this.timeout * 1000).build();
//        CloseableHttpClient client =
//                HttpClientBuilder.create().setDefaultRequestConfig(config_timeouts).build();
        CloseableHttpClient client = HttpClients.custom()
                .disableAutomaticRetries() // no retries
                .setDefaultRequestConfig(config_timeouts).build();

        String url = buildURL(this.myInstance);

        HttpGet httpget = new HttpGet(url);
        httpget.addHeader("User-Agent", this.agent);

        CloseableHttpResponse httpresponse = null;
        int statuscode;
        try {
            httpresponse = client.execute(httpget);
            statuscode = httpresponse.getStatusLine().getStatusCode();

        } catch (IOException e) {
            e.printStackTrace();
            statuscode = 504;
        }

        if(statuscode == 200){
            if (!this.myInstance.isHealthy()){
                this.myInstance.incHealthychecks();
                if (this.myInstance.getHealthychecks() == MyInstance.getNeededHealthyChecks()){
                    this.myInstance.setHealthy(true);
                    System.out.println(String.format("Instance: %s with DNS: %s is healthy.",
                            myInstance.getId(), myInstance.getIp()));
                }
            }
            this.myInstance.resetUnealthychecks(); //TODO: change this
        }
        else {
            this.myInstance.incUnhealthychecks();
            if (this.myInstance.isHealthy()){
                this.myInstance.setHealthy(false);
                System.out.println(String.format("Instance: %s with DNS: %s is unhealthy.",
                        myInstance.getId(), myInstance.getIp()));
            }
            this.myInstance.resetHealthychecks();
        }
    }
}