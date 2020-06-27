package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import pt.ulisboa.tecnico.cnv.requestinfo.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CostPredictor {
    private static CostPredictor predictor;
    private ConcurrentHashMap<String, CachedSample> LB_cache = new ConcurrentHashMap<String, CachedSample>();

    private double MAXIMUM_COST_25x25 = 38000;
    private double MAXIMUM_COST_16x16 = 13000;
    private double MAXIMUM_COST_9x9 = 3600;

    private double NO_RESULT_COEF = 0.5;

    public ConcurrentHashMap<String, CachedSample> getLB_cache() { return LB_cache; }

    public void printCache() {
        System.out.println("After inserting sample in cache ... samples in hashmap");
        for (String name : LB_cache.keySet()) {
            String key = name;
            String value = LB_cache.get(name).toString();
            System.out.println(key + " " + value);
        }
        System.out.println("\n");
    }

    public void initCache(List<Map<String, AttributeValue>> table) {
        for (Map<String, AttributeValue> sample : table) {
            String primaryKey = sample.get("INDEX").getS();
            String puzzle_name = sample.get("puzzle name").getS();
            String strategy = sample.get("strategy").getS();
            int X = Integer.parseInt(sample.get("sizeX").getN());
            int Y = Integer.parseInt(sample.get("sizeY").getN());
            int miss_ele = Integer.parseInt(sample.get("missing elements").getN());
            int range = Integer.parseInt(sample.get("range").getN());
            int cost = Integer.parseInt(sample.get("cost").getN());

            Request request;
            if (strategy.equals("BFS")) {
                request = new RequestBFS(puzzle_name, strategy, X, Y, miss_ele);
            } else if (strategy.equals("CP")) {
                request = new RequestCP(puzzle_name, strategy, X, Y, miss_ele);
            } else {
                request = new RequestDLX(puzzle_name, strategy, X, Y, miss_ele);
            }

            Measurement newCachedMeasurement = new Measurement(request, cost);
            this.insertFullSampleInCache(newCachedMeasurement, range, primaryKey);
        }
    }


    private String createPrimaryKey(int X, int Y, int range, String puzzle_name, String strategy){
        if (strategy.equals("DLX"))
            return String.valueOf(X)+"x"+String.valueOf(Y)+strategy;
        else
            return String.valueOf(X)+"x"+String.valueOf(Y)+strategy+"R"+String.valueOf(range)+puzzle_name;
    }

    public void insertFullSampleInCache(Measurement measure, int range, String primaryKey){
        CachedSample newCacheSample = new CachedSample(measure, range);
        this.LB_cache.put(primaryKey, newCacheSample);
    }

    public void insertInLBCache(Measurement measure){
        Request request = measure.getRequest();
        double X = request.getSizeX();
        double Y = request.getSizeY();
        String name = request.getPuzzle_name();
        double area = X * Y;
        double miss = request.getMiss_ele();
        String strategy = request.getStrategy();
        int range = calculateRange(miss, area);

        String primaryKey = createPrimaryKey((int)X, (int)Y, range, name, strategy);

        CachedSample newCacheSample = new CachedSample(measure, range);
        CachedSample oldSample = this.LB_cache.putIfAbsent(primaryKey, newCacheSample);
        if (oldSample == null){
            System.out.println("MESSAGE: A new sample was added to cache.");
            return;
        }
        else {
            System.out.println("MESSAGE: A sample was updated in cache.");
            oldSample.updateCachedSample(measure);
            this.LB_cache.replace(primaryKey, oldSample);
        }
    }


    public static CostPredictor getCostPredictor(){
        if (predictor == null){
            System.out.println("MESSAGE: There is no cache. Creating one ...");
            predictor = new CostPredictor();
        }
        return predictor;
    }

    private int calculateRange(double miss, double area){
        int range;
        if (miss/area <= (0.33)) range = 1;
        else if ((miss/area) > (0.33) && (miss/area) <= (0.66)) range = 2;
        else range = 3;
        return range;
    }

    protected synchronized double predictCost(Request request){
        double X = request.getSizeX();
        double Y = request.getSizeY();
        String name = request.getPuzzle_name();
        double area = X*Y;
        double miss = request.getMiss_ele();
        String strategy = request.getStrategy();
        int range = calculateRange(miss, area);

        String primaryKey = createPrimaryKey((int)X, (int)Y, range, name, strategy);

        if (request.getStrategy().equals("DLX")){
            CachedSample predicted = LB_cache.get(primaryKey);
            if (predicted != null) return predicted.getMeasurement().getCost();
        }
        else {
            CachedSample predicted = LB_cache.get(primaryKey);
            if (predicted != null) return predicted.getMeasurement().getCost();
            else{
                for (CachedSample s : LB_cache.values()) {
                    Measurement m = s.getMeasurement();
                    Request cache_request = m.getRequest();
                    int cache_request_range = s.getRange();

                    if ((cache_request.getSizeX() == X) &&
                        (cache_request.getSizeY() == Y) &&
                        (cache_request.getStrategy().equals(strategy)) &&
                        (cache_request_range == range))
                        return m.getCost();
                }
            }
        }

        if ((X == 9) && (Y == 9)) return MAXIMUM_COST_9x9*NO_RESULT_COEF;
        else if((X==16) && (Y==16)) return MAXIMUM_COST_16x16*NO_RESULT_COEF;
        else if ((X==25)&&(Y==25)) return MAXIMUM_COST_25x25*NO_RESULT_COEF;
        else return MAXIMUM_COST_25x25*0.75;
    }
}
