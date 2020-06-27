package pt.ulisboa.tecnico.cnv.requestinfo;

import java.util.ArrayList;
import java.util.List;

public class RequestBFS extends Request {

    double BFS_constant = 1.8;
    double miss_ele_constant = 2;

    public RequestBFS(String puzzle_name, String strategy, int sizeX, int sizeY, int miss_ele) {
        super(puzzle_name, strategy, sizeX, sizeY, miss_ele);
    }

    @Override
    public double calculateCost(long methods) {
        return BFS_constant*(methods + methods*miss_ele_constant*this.perc_miss_ele);
    }
}
