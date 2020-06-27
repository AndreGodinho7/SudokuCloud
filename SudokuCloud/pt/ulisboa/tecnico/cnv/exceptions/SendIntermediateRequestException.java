package pt.ulisboa.tecnico.cnv.exceptions;

import pt.ulisboa.tecnico.cnv.loadbalancer.MyInstance;
import pt.ulisboa.tecnico.cnv.requestinfo.Request;

public class SendIntermediateRequestException extends Exception {
    private final MyInstance leastLoadedInstance;

    public SendIntermediateRequestException(MyInstance leastLoadedInstance, Request r) {
        System.out.println(String.format("MESSAGE: The request "+r.toString()+" will be sent to %s instance with DNS %s",
                leastLoadedInstance.getId(), leastLoadedInstance.getIp()));
        this.leastLoadedInstance = leastLoadedInstance;
    }
    public MyInstance getLeastLoadedInstance() {
        return leastLoadedInstance;
    }
}
