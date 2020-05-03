package pt.ulisboa.tecnico.cnv.requestinfo;

import java.util.ArrayList;
import java.util.List;

public class RequestDLX extends Request {

    public RequestDLX(String puzzle_name, String strategy, int sizeX, int sizeY, int miss_ele) {
        super(puzzle_name, strategy, sizeX, sizeY, miss_ele);
    }

    @Override
    public double calculateCost(long methods) {
        return (double) methods;
    }
}
