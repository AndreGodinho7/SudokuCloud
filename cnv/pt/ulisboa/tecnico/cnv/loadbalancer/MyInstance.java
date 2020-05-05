package pt.ulisboa.tecnico.cnv.loadbalancer;

public class MyInstance {
    private String id;

    private String host;

    private int port;

    private double workload;

    private int runningRequests;

    public MyInstance(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.workload = 0;
        this.runningRequests = 0;
    }

    public void newRequest() {
        this.runningRequests++;
    }

    public void finishRequest() {
        if(this.runningRequests > 0) {
            this.runningRequests--;
        }
    }
}
