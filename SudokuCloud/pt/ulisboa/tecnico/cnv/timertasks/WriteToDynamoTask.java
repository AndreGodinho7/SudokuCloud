package pt.ulisboa.tecnico.cnv.timertasks;

import pt.ulisboa.tecnico.cnv.loadbalancer.CostPredictor;
import pt.ulisboa.tecnico.cnv.loadbalancer.InstanceManager;
import java.util.TimerTask;

public class WriteToDynamoTask extends TimerTask {
    private static InstanceManager instanceManager = InstanceManager.getInstance();
    private static CostPredictor predictor = CostPredictor.getCostPredictor();

    @Override
    public void run() {
        System.out.println("\n\n");
        System.out.println("TASK: WRITE TO DYNAMO DB");
        instanceManager.writeCacheToDynamoDB(predictor.getLB_cache());
        System.out.println("\n\n");
    }
}
