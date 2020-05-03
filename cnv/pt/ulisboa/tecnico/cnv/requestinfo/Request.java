package pt.ulisboa.tecnico.cnv.requestinfo;

public abstract class Request {
    protected String puzzle_name;
    protected String strategy;
    protected double sizeX;
    protected double sizeY;
    protected int miss_ele;

    public Request(String puzzle_name, String strategy, int sizeX, int sizeY, int miss_ele) {
        this.puzzle_name = puzzle_name;
        this.strategy = strategy;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.miss_ele = miss_ele;
    }

    public abstract double calculateCost(long methods);

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
