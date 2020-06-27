package pt.ulisboa.tecnico.cnv.timertasks;

import pt.ulisboa.tecnico.cnv.loadbalancer.InstanceManager;
import pt.ulisboa.tecnico.cnv.loadbalancer.MyInstance;

import java.util.Collection;
import java.util.TimerTask;

public class HealthChecksTask extends TimerTask {
    private static InstanceManager instanceManager = InstanceManager.getInstance();

    @Override
    public void run() {
        Collection<MyInstance> myInstances = instanceManager.getInstancesMap().values();
        System.out.print("\n\n");
        System.out.println(String.format("TASK: HEALTH CHECKS - starting to send health checks to %d instances",
                myInstances.size()));

        for (MyInstance instance: myInstances){
            HealthCheckThread t = new HealthCheckThread(instance);
            t.start();
        }
    }
}
