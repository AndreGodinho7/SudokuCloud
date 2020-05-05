package pt.ulisboa.tecnico.cnv.loadbalancer;

import pt.ulisboa.tecnico.cnv.requestinfo.*;

import java.util.HashMap;
import java.util.Map;

public class CostPredictor {
    private Map<Integer, Measurement> LB_cache = new HashMap<Integer, Measurement>();

    private void init_sample_BFS(long methods, long thread, String puzzle, String solver, int X, int Y ,int miss){
        Request r = new RequestBFS(puzzle, solver, X, Y, miss);
        Measurement m = new Measurement(thread, r);
        m.set_methods(methods);
        m.calculateCost();
        LB_cache.put((int) m.getCost(), m);
    }
    private void init_sample_CP(long methods, long thread, String puzzle, String solver, int X, int Y ,int miss){
        Request r = new RequestCP(puzzle, solver, X, Y, miss);
        Measurement m = new Measurement(thread, r);
        m.set_methods(methods);
        m.calculateCost();
        LB_cache.put((int) m.getCost(), m);
    }
    private void init_sample_DLX(long methods, long thread, String puzzle, String solver, int X, int Y ,int miss){
        Request r = new RequestDLX(puzzle, solver, X, Y, miss);
        Measurement m = new Measurement(thread, r);
        m.set_methods(methods);
        m.calculateCost();
        LB_cache.put((int) m.getCost(), m);
    }

    public CostPredictor(){
        long threadID = Thread.currentThread().getId();
        init_sample_BFS(943, threadID, "9x9_01", "BFS",9,9,81);
        init_sample_CP(766, threadID, "9x9_01", "CP",9,9,81);
        init_sample_DLX(3580, threadID, "9x9_01", "DLX",9,9,81);

        init_sample_BFS(5385, threadID, "16x16_02", "BFS",16,16,256);
        init_sample_CP(4389, threadID, "16x16_02", "CP",16,16,256);
        init_sample_DLX(12954, threadID, "16x16_02", "DLX",16,16,256);

        init_sample_BFS(8910, threadID, "9x9_01", "BFS",25,25,625);
        init_sample_CP(7361, threadID, "9x9_01", "CP",25,25,625);
        init_sample_DLX(36784, threadID, "9x9_01", "DLX",25,25,625);
    }

    private int calculateRange(double miss, double area){
        int range;
        if (miss/area <= (1/3)) range = 1;
        else if ((miss/area) > (1/3) && (miss/area) <= (2/3)) range = 2;
        else range = 3;
        return range;
    }

    protected double predictCost(Request request){
        double X = request.getSizeX();
        double Y = request.getSizeY();
        double area = X*Y;
        double miss = request.getMiss_ele();
        String strategy = request.getStrategy();
        int range = calculateRange(miss, area);

        double cost = 0;

        for (Measurement m : LB_cache.values()){
            Request cache_request = m.getRequest();
            int cache_request_range = calculateRange(cache_request.getMiss_ele(),
                                                cache_request.getSizeX()*cache_request.getSizeY());
            if (m.getCost() > cost) cost = m.getCost();

            if ((cache_request.getSizeX() == X) &&
                (cache_request.getSizeY() == Y) &&
                (cache_request.getStrategy().equals(strategy)) &&
                (cache_request_range == range)){
                return m.getCost();
            }
        }
        return 2*cost;
    }
}
