package pt.ulisboa.tecnico.cnv.timertasks;

import pt.ulisboa.tecnico.cnv.loadbalancer.InstanceManager;
import pt.ulisboa.tecnico.cnv.loadbalancer.MyInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

public class AutoScallingTask extends TimerTask {
    private final double SCALEUP_LOAD_UTILIZATION = 0.5;
    private final double SCALEDOWN_LOAD_UTILIZATION = 0.3;
    private final int avgLoadSamples = 8;
    private List<Double> avgLoads = new ArrayList<>(avgLoadSamples);
    private static InstanceManager instanceManager = InstanceManager.getInstance();

    private double calculateAverageSamplesLoad(){
        double sum = 0;
        double average;
        if (!avgLoads.isEmpty()) {
            for (double avg : avgLoads) sum += avg;
            average = sum / avgLoadSamples;
            return average;
        }
        return 0;
    }

    @Override
    public void run() {
        int healthy =  instanceManager.getNumHealthyInstances();
        System.out.print("\n\n");
        try {
            if (avgLoads.size() == avgLoadSamples) {
                System.out.println(String.format("TASK: AUTO SCALING - calculating average LOAD in %d instances", healthy));
                if (healthy >= instanceManager.getMIN_INSTANCES()) {
                    double avgLoadSamples = calculateAverageSamplesLoad();
                    System.out.println("TASK: AUTO SCALING - average load = "+avgLoadSamples);
                    if (avgLoadSamples >= SCALEUP_LOAD_UTILIZATION) {
                        System.out.println("TASK: AUTO SCALING - SCALE UP ACTIVATED");
                        instanceManager.startNewInstance();
                        instanceManager.setIpInstances();
                    } else if (avgLoadSamples <= SCALEDOWN_LOAD_UTILIZATION) {
                        System.out.println("TASK: AUTO SCALING - SCALE DOWN ACTIVATED");
                        if (healthy > instanceManager.getMIN_INSTANCES()) {
                            System.out.println("TASK: AUTO SCALING - System will check if there is a instance with 0 requests to remove.");
                            instanceManager.removeInstance();
                        } else {
                            System.out.println("TASK: AUTO SCALING - Load average is below scaledown threshold but system has the minimum number of active instances.");
                        }
                    }
                } else {
                    System.out.println(String.format("TASK: AUTO SCALING - There aren't enough healthy instances. Found %d healthy instances.", healthy));
                }
                avgLoads.clear(); // reset list
            }

            System.out.println(String.format("TASK: AUTO SCALING - sampling average LOAD in %d instances", healthy));
            double current_load = instanceManager.calculateAverageLoad();
            avgLoads.add(current_load);
            System.out.println(String.format("TASK: AUTO SCALING - #Current samples: %d", avgLoads.size()));
            System.out.println("TASK: AUTO SCALING - Samples: " + avgLoads.toString());
        }catch (Exception e){
            System.out.println("TASK: AUTO SCALING - ERROR");
        }
    }
}