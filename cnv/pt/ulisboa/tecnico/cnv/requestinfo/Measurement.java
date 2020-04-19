package pt.ulisboa.tecnico.cnv.requestinfo;

import java.util.Objects;

public class Measurement {
    private long i_count, b_count, m_count;
    private long threadID;
    private Request request;

    public Measurement(long threadID, Request request){
        this.threadID = threadID;
        this.i_count = 0;
        this.b_count = 0;
        this.m_count = 0;
        this.request = request;
    }

    public long getI_count() {
        return i_count;
    }

    public void incInstructions(int incr){
        this.i_count += incr;
    }
    public void incBlocks(){ this.b_count++; }
    public void incMethods(){
        this.m_count++;
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        Measurement that = (Measurement) object;
        return i_count == that.i_count &&
                b_count == that.b_count &&
                m_count == that.m_count;
    }

    @Override
    public String toString() {
        return  "Measurement\n              { " +
                "threadID=" + threadID +
                ", i_count=" + i_count +
                ", b_count=" + b_count +
                ", m_count=" + m_count +
                ", request=" + request;
    }
}