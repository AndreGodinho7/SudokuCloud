package pt.ulisboa.tecnico.cnv.requestinfo;

public class Measurement {
    private long m_count;
    private double cost;
    private long threadID;
    private Request request;

    public Measurement(long threadID, Request request){
        this.threadID = threadID;
        this.m_count = 0;
        this.request = request;
    }

    public Measurement(Request r, double cost){
        this.request = r;
        this.cost = cost;
    }

    public long getThreadID() {
        return threadID;
    }

    public long getM_count() {
        return m_count;
    }

    public Request getRequest() {
        return request;
    }

    public double getCost() {
        return cost;
    }

    public void incMethods(){
        this.m_count++;
    }

    public void set_methods(long methods){
        this.m_count = methods;
    }

    public void calculateCost(){
        this.cost = request.calculateCost(m_count);
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        Measurement that = (Measurement) object;
        return m_count == that.m_count;
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "m_count=" + m_count +
                ", cost=" + cost +
                ", threadID=" + threadID +
                ", request=" + request +
                '}';
    }
}