package pt.ulisboa.tecnico.cnv.requestinfo;

public class Request {
    private String puzzle_name;
    private String strategy;
    private int sizeX;
    private int sizeY;
    private int miss_ele;

    public Request(String puzzle_name, String strategy, int sizeX, int sizeY, int miss_ele) {
        this.puzzle_name = puzzle_name;
        this.strategy = strategy;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.miss_ele = miss_ele;
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
