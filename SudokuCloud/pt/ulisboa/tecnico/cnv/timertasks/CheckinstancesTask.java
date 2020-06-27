package pt.ulisboa.tecnico.cnv.timertasks;

import java.util.*;

import com.amazonaws.services.ec2.model.*;
import pt.ulisboa.tecnico.cnv.loadbalancer.AWSManager;
import pt.ulisboa.tecnico.cnv.loadbalancer.InstanceManager;


public class CheckinstancesTask extends TimerTask {
    private static InstanceManager manager = InstanceManager.getInstance();
    private final int MAX_UNHEALTHY_CHECKS = 2;

    @Override
    public void run() {
        System.out.print("\n\n");
        System.out.println("TASK: CHECK INSTANCES - Cleaning or booting instances.");
        AWSManager aws = manager.getAws();

        List<Instance> instances = aws.getAllInstances();

        if ((instances.size() > 0) &&(manager.getInstancesMap().size() > 0)) {
            Map<String, String> aux = new HashMap<>();
            for (Instance i : instances) {
                aux.put(i.getInstanceId(), i.getInstanceId());
            }
            for (String key : manager.getInstancesMap().keySet()) {
                if (!aux.containsKey(key)) {
                    System.out.println("TASK: CHECK INSTANCES - instance " + key +" is not in EC2 instances. Remove from hashmap.");
                    manager.removeFromMap(key);
                }
            }
        }


        Iterator<Instance> iter = instances.iterator();
        try {
            while (iter.hasNext()) {
                Instance cur = iter.next();
                if (manager.getMyInstance(cur.getInstanceId()).getUnhealthychecks() >= MAX_UNHEALTHY_CHECKS) {
                    System.out.println(String.format("TASK: CHECK INSTANCES - Instance %s is unhealthy. It will be removed",
                            cur.getInstanceId()));
                    manager.removeSpecificinstance(cur.getInstanceId());
                }
            }
            if (instances.size() < manager.getMIN_INSTANCES()){
                System.out.println(String.format("TASK: CHECK INSTANCES - There are only %d instances. Creating a new one.", instances.size()));
                manager.startNewInstance();
            }
        }catch (Exception e){
            System.out.println("TASK: CHECK INSTANCES - ERROR");
            e.printStackTrace();
        }
    }
}
