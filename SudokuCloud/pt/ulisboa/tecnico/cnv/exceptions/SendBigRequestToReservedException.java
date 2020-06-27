package pt.ulisboa.tecnico.cnv.exceptions;

import pt.ulisboa.tecnico.cnv.loadbalancer.BigRequestsManager;
import pt.ulisboa.tecnico.cnv.loadbalancer.InstanceManager;
import pt.ulisboa.tecnico.cnv.loadbalancer.MyInstance;
import pt.ulisboa.tecnico.cnv.requestinfo.Request;

public class SendBigRequestToReservedException extends Exception {
    private final MyInstance newInstance;
    private static InstanceManager manager = InstanceManager.getInstance();
    private static BigRequestsManager overtakenRequests = BigRequestsManager.getBigRequestsManager();

    public SendBigRequestToReservedException(double predictedCost, double relativePredictedLoad, Request r) {
        System.out.println("LOAD BALANCER WARNING: Initializing new reserved instance.");
        overtakenRequests.removeBigRequest(Thread.currentThread().getId());
        MyInstance chosen = manager.startNewReservedInstance();
        chosen.setReserved(true);
        chosen.setWorkload(predictedCost); // 0 + predicted cost
        chosen.setRelative_workload(relativePredictedLoad);
        chosen.addNewRequest();
        System.out.println(String.format("LOAD BALANCER WARNING: The RESERVED request "+r.toString()+" will be sent to %s instance with DNS %s AFTER HEALTH CHECKS",
                chosen.getId(), chosen.getIp()));
        this.newInstance = chosen;
    }
    public MyInstance getLeastLoadedInstance() {
        return newInstance;
    }
}

