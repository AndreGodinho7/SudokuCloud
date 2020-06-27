package pt.ulisboa.tecnico.cnv.requestinfo;

public abstract class Request {
    protected String puzzle_name;
    protected String strategy;
    protected double sizeX;
    protected double sizeY;
    protected double perc_miss_ele;
    protected int miss_ele;
    protected String request_type;

    protected double overtakenValue;
    protected double max_overtaken_value;
    protected double predicted_cost;

    //Get all prime numbers in the interval
    private int getPrimes(int min, int max) {
        boolean [] primes = new boolean[max];
        for(int i = min; i < max; i++) {
            primes[i] = true;
        }
        float limit = (float) Math.sqrt(max);
        for(float i = min; i < limit; i++) {
            if(primes[(int)i]) {
                for(float j = i * i; j < max; j += i) {
                    primes[(int)j] = false;
                }
            }
        }
        int sum = 0;
        for(boolean b : primes) {
            sum += b ? 1 : 0;
        }
        return  sum;
    }

    private double calc_percentage_miss_elements(int X, int Y, int miss){
        int primes = getPrimes(2, miss);
        return ((double) primes)/(X*Y);
    }

    public Request(String puzzle_name, String strategy, int sizeX, int sizeY, int miss_ele) {
        this.puzzle_name = puzzle_name;
        this.strategy = strategy;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.miss_ele = miss_ele;
        this.perc_miss_ele = calc_percentage_miss_elements(sizeX, sizeY, miss_ele);
        this.predicted_cost = 0;
        this.overtakenValue = 0;
    }

    public String getPuzzle_name() {
        return puzzle_name;
    }

    public String getStrategy() {
        return strategy;
    }

    public double getSizeY() {
        return sizeY;
    }

    public double getSizeX() {
        return sizeX;
    }

    public int getMiss_ele() {
        return miss_ele;
    }

    public double getPerc_miss_ele() { return perc_miss_ele; }

    public double getPredicted_cost() { return predicted_cost; }

    public String getRequest_type() { return request_type; }

    public abstract double calculateCost(long methods);

    public void setPredicted_cost(double predicted_cost) { this.predicted_cost = predicted_cost; }

    public void updateOvertaken(double cost) {this.overtakenValue += cost;}

    public void setRequest_type(String request_type) { this.request_type = request_type; }

    public void setMax_overtaken_value(double coef){this.max_overtaken_value = this.predicted_cost*coef;}

    public boolean maxOverTakenBreached(){
        if(this.max_overtaken_value < this.overtakenValue){
            return true;
        }
        else return false;
    }

    @Override
    public String toString() {
        return "Request{" +
                "puzzle_name='" + puzzle_name + '\'' +
                ", strategy='" + strategy + '\'' +
                ", sizeX=" + sizeX +
                ", sizeY=" + sizeY +
                ", miss_ele=" + miss_ele +
                '}';
    }
}
