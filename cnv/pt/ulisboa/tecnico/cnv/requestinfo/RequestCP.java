package pt.ulisboa.tecnico.cnv.requestinfo;


import java.util.ArrayList;
import java.util.List;

public class RequestCP extends Request {

    double CP_constant = (long) 1.8;
    double miss_ele_constant = 2;

    public RequestCP(String puzzle_name, String strategy, int sizeX, int sizeY, int miss_ele) {
        super(puzzle_name, strategy, sizeX, sizeY, miss_ele);
    }

    @Override
    public double calculateCost(long methods) {
        return CP_constant*(methods + methods*miss_ele_constant*this.perc_miss_ele);
    }
}
