package pt.ulisboa.tecnico.cnv.loadbalancer;

import pt.ulisboa.tecnico.cnv.requestinfo.Measurement;

public class CachedSample {
    private Measurement measurement;
    private double averageTerms;
    private double average_cost;

    private int range;

    public CachedSample(Measurement measurement, int range) {
        this.measurement = measurement;
        this.average_cost = measurement.getCost();
        this.averageTerms = 1;
        this.range = range;
    }

    public Measurement getMeasurement() { return measurement; }
    public double getAverageTerms() { return averageTerms; }
    public int getRange() { return range; }

    private void incAverageTerms(){
        this.averageTerms++;
    }
    private void updateAverage(Measurement m){
        this.average_cost = this.average_cost + ((m.getCost()-this.average_cost)/(this.averageTerms));
    }

    public void updateCachedSample(Measurement m){
        this.incAverageTerms();
        this.updateAverage(m);
    }

    @Override
    public String toString() {
        return "CachedSample{" +
                "measurement=" + measurement +
                ", averageTerms=" + averageTerms +
                ", average_cost=" + average_cost +
                ", range=" + range +
                '}';
    }
}
