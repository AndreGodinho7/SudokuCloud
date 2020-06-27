package pt.ulisboa.tecnico.cnv.loadbalancer;

public class MyInstance {
    private String id;
    private String ip;
    private int port;
    private double workload;
    private double relative_workload;
    private int runningRequests;
    private int healthychecks;
    private int unhealthychecks;
    private boolean healthy;
    private boolean reserved;

    private static final int NEEDED_HEALTHY_CHECKS = 3;

    public MyInstance(String id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.workload = 0;
        this.relative_workload = 0;
        this.runningRequests = 0;
        this.healthychecks = 0;
        this.unhealthychecks = 0;
        this.healthy = false;
        this.reserved = false;
    }

    // getters
    public int getRunningRequests() { return runningRequests; }
    public double getWorkload() { return workload; }
    public String getId() { return id; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public boolean isHealthy() { return healthy; }
    public int getHealthychecks() { return healthychecks; }
    public int getUnhealthychecks() { return unhealthychecks; }
    public double getRelative_workload() { return relative_workload; }
    public static int getNeededHealthyChecks() { return NEEDED_HEALTHY_CHECKS; }
    public boolean isReserved() { return reserved; }

    // setters
    public void setWorkload(double workload) { this.workload = workload; }
    public void setIp(String ip) { this.ip = ip; }
    public void setHealthy(boolean status) { this.healthy = status; }
    public void setRelative_workload(double relative_workload) { this.relative_workload = relative_workload; }
    public void setReserved(boolean reserved) { this.reserved = reserved; }

    public void incHealthychecks(){this.healthychecks++;}
    public void incUnhealthychecks(){
        this.unhealthychecks++;
        if (this.unhealthychecks >= 1) System.out.println(String.format("ERROR: Instance %s with DNS %s has %d UNHEALTHY checks.",
                this.getId(),this.getIp(),this.getUnhealthychecks()));
    }
    public void resetHealthychecks(){this.healthychecks = 0;}
    public void resetUnealthychecks(){this.unhealthychecks = 0;}

    public void addNewRequest() {
        this.runningRequests++;
    }

    public void finishRequest() {
        if(this.runningRequests > 0) {
            this.runningRequests--;
        }
    }
}
