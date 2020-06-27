package pt.ulisboa.tecnico.cnv.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import pt.ulisboa.tecnico.cnv.requestinfo.*;
import pt.ulisboa.tecnico.cnv.solver.Solver;
import pt.ulisboa.tecnico.cnv.solver.SolverArgumentParser;
import pt.ulisboa.tecnico.cnv.solver.SolverFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Executors;


public class WebServer {
	public static final MeasurementsManager WebServerManager = MeasurementsManager.getManager();

	public static void main(final String[] args) throws Exception {

		//final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8000), 0);

		final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

		server.createContext("/sudoku", new MyHandler());
        server.createContext("/health", new HealthHandler());

		// be aware! infinite pool of threads!
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		System.out.println(server.getAddress().toString());
	}

	protected static String parseRequestBody(InputStream is) throws IOException {
        InputStreamReader isr =  new InputStreamReader(is,"utf-8");
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
	static class MyHandler implements HttpHandler {
		@Override
		public void handle(final HttpExchange t) throws IOException {
			// Get the query.
			JSONArray solution = null;
			try {

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
//				newArgs.add("-b");
//				newArgs.add(parseRequestBody(t.getRequestBody()));

				newArgs.add("-d");

				// Store from ArrayList into regular String[].
				final String[] args = new String[newArgs.size()];
				int i = 0;
				for (String arg : newArgs) {
					System.out.println(String.format("Arg i: %d ",i)+arg);
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

				Measurement measure = new Measurement(Thread.currentThread().getId(), request);
				WebServerManager.insertMeasurement(measure);
				//			try {
				// Get user-provided flags.
				SolverArgumentParser ap = null;
				try{
					ap = new SolverArgumentParser(args);
				}catch(Exception e) {
					System.out.println(e);
					return;
				}
				// Create solver instance from factory.
				final Solver s = SolverFactory.getInstance().makeSolver(ap);

				//Solve sudoku puzzle
				solution = s.solveSudoku();
			}
			catch (Exception e){
				e.printStackTrace();
			}
	//			}catch (Exception e){
//				StringWriter sw = new StringWriter();
//				e.printStackTrace(new PrintWriter(sw));
//				String exceptionAsString = sw.toString();
//				t.sendResponseHeaders(500, exceptionAsString.length());
//			}

			// Send response to browser.
			final Headers hdrs = t.getResponseHeaders();

            //t.sendResponseHeaders(200, responseFile.length());

			///hdrs.add("Content-Type", "image/png");
            hdrs.add("Content-Type", "application/json");

			hdrs.add("Access-Control-Allow-Origin", "*");

            hdrs.add("Access-Control-Allow-Credentials", "true");
			hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
			hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

			WebServerManager.calcRequestCost(Thread.currentThread().getId());
			Measurement m = WebServerManager.getMeasurement(Thread.currentThread().getId());
			int cost = (int) m.getCost();
			System.out.println(cost);
			System.out.println((solution.toString()+String.format("%d",cost)));
			t.sendResponseHeaders(200, (solution.toString()+String.format("--%d",cost)).length());


			/// Calculate cost with measurements
			WebServerManager.removeMeasurement(Thread.currentThread().getId());

            final OutputStream os = t.getResponseBody();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(solution.toString()+String.format("--%d",cost)); // solution
            osw.flush();
            osw.close();

			os.close();

			System.out.println("> Sent response to " + t.getRemoteAddress().toString());
		}
	}

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, 0);
            OutputStream os = t.getResponseBody();
            os.write("OK".getBytes());
            os.close();
        }
    }
}