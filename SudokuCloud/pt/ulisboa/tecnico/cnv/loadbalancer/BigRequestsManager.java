package pt.ulisboa.tecnico.cnv.loadbalancer;

import pt.ulisboa.tecnico.cnv.requestinfo.Request;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BigRequestsManager {
    private static BigRequestsManager manager;
    private Map<Long, Request> bigRequestsMap = new ConcurrentHashMap<>();

    public static BigRequestsManager getBigRequestsManager() {
        if(manager == null) {
            System.out.println("MESSAGE: There is no BigRequestsManager. Creating one ... \n");
            manager = new BigRequestsManager();
        }
        return manager;
    }

    public synchronized void updateOvertakingRequests(String typeofquest, double cost){
        Collection<Request> requests = bigRequestsMap.values();
        for (Request request : requests){
            request.updateOvertaken(cost);
        }
        System.out.println(String.format("WARNING: A " +typeofquest+ " request has overtaken %d big requests.",requests.size()));

    }

    public synchronized void insertBigRequest(long threadID, Request r){
        this.bigRequestsMap.put(threadID, r);
    }

    public synchronized void removeBigRequest(long threadID){
        this.bigRequestsMap.remove(threadID);
    }

    public synchronized boolean isBigRequestWaiting(){
        if (this.bigRequestsMap.size() > 0) return true;
        else return false;
    }
}
