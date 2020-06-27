package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.securityhub.model.Network;
import com.amazonaws.util.EC2MetadataUtils;
import pt.ulisboa.tecnico.cnv.exceptions.*;
import pt.ulisboa.tecnico.cnv.requestinfo.Measurement;
import pt.ulisboa.tecnico.cnv.requestinfo.Request;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceManager {
    private static InstanceManager manager;

    private Map<String, MyInstance> instancesMap = new ConcurrentHashMap<>();
    private AWSManager aws;

    private static double MAXIMUM_COST = 38000;
    private static final double MAXIMUM_LOAD = 1;
    private static final double BIG_REQUEST_THRESHOLD = 0.5;
    private static final double SMALL_REQUEST_THRESHOLD = 0.15 ;

    private final int PORT = 8000;
    private final int MIN_INSTANCES = 2;
    private final int GRACE_PERIOD = 60 *1000;


    private InstanceManager() {
        System.out.println("Creating InstanceManager ... \n");
        this.aws = new AWSManager();
        aws.initDynamoTable();
        System.out.println("Getting all running EC2 instances ... \n");
        List<Instance> instances = this.aws.getAllInstances();

        // TODO: Duvida: when initializing, instances have no Workload bc any MyInstance is initialized
        if(instances.size() < this.MIN_INSTANCES) {
            for(int i = 0; i < this.MIN_INSTANCES - instances.size(); i++) {
                this.startNewInstance();
            }
//            this.setIpInstances(); //TODO : improve this by set ip for each instance separatly
        }
        if (instances.size() == this.MIN_INSTANCES){
            for (Instance i : instances){
                String id = i.getInstanceId();
                String ip = i.getPublicIpAddress();
                System.out.println(String.format("MESSAGE: BOOTING instance %s with IP address: %s \n", id, ip));
                MyInstance myInstance = new MyInstance(id, ip, this.PORT);

                this.instancesMap.put(id, myInstance);
            }
        }
    }

    public AWSManager getAws() {
        return aws;
    }

    public int getMIN_INSTANCES() { return MIN_INSTANCES; }
    public Map<String, MyInstance> getInstancesMap() { return instancesMap; }
    public MyInstance getMyInstance(String id){
        return this.instancesMap.get(id);
    }

    public boolean isInstanceinMap(String id){
        if (this.instancesMap.containsKey(id)) return true;
        else return false;
    }

    public static InstanceManager getInstance() {
        if(manager == null) {
            System.out.println("MESSAGE: There is no InstanceManager. Creating one ... \n");
            manager = new InstanceManager();
        }
        return manager;
    }

    public static double getMaximumLoad() { return MAXIMUM_LOAD; }
    public static double getMaximumCost() { return MAXIMUM_COST; }
    public static void setMaximumCost(double maximumCost) { MAXIMUM_COST = maximumCost; }

    public synchronized void startNewInstance() {
        Instance newInstance = this.aws.startNewInstance();
        try {
            Thread.sleep(GRACE_PERIOD);

            String id = newInstance.getInstanceId();
            String ip = newInstance.getPublicIpAddress();
            System.out.println(String.format("MESSAGE: BOOTING instance %s with IP address: %s \n", id, ip));
            MyInstance myInstance = new MyInstance(id, ip, this.PORT);
            this.instancesMap.put(id, myInstance);
            this.setInstance(id);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized MyInstance startNewReservedInstance(){
        Instance newInstance = this.aws.startNewInstance();
        MyInstance myInstance = null;
        try {
            Thread.sleep(GRACE_PERIOD);

            String id = newInstance.getInstanceId();
            String ip = newInstance.getPublicIpAddress();
            System.out.println(String.format("MESSAGE: BOOTING instance %s with IP address: %s \n", id, ip));
            myInstance = new MyInstance(id, ip, this.PORT);
            this.instancesMap.put(id, myInstance);
            this.setInstance(id);
        } catch(Exception e) {
            System.out.println("ERROR: Error in booting reserved instance.");
            System.out.println(e.getMessage());
        }
        return myInstance;
    }

    public synchronized void removeInstance(){
        Collection<MyInstance> instances = this.instancesMap.values();

        for (MyInstance instance : instances){
            if (instance.getRunningRequests() == 0){
                instance.setHealthy(false);
                this.aws.removeInstance(instance.getId());
                this.instancesMap.remove(instance.getId());
                System.out.println(String.format("MESSAGE: FOUND instance with 0 requests, removing instance %s with DNS %s.",
                        instance.getId(), instance.getIp()));
                break;
            }
        }
    }

    public void removeSpecificinstance(String id){
        this.aws.removeInstance(id);
        this.instancesMap.remove(id);
        System.out.println(String.format("TASK - CheckInstances - Removing instance %s.",
                id));
    }

    public void removeFromMap(String id){
        this.instancesMap.remove(id);
    }

    public synchronized void setIpInstances(){
        this.instancesMap = this.aws.setIPInstances(this.instancesMap);
    }

    public synchronized void setInstance(String id){
        this.instancesMap = this.aws.setIPInstance(this.instancesMap, id);
    }

    public synchronized void leastLoadedInstance(double predicted_cost, boolean reserve, Request r) throws NoInstanceAvailableException, SendBigRequestException, SendIntermediateRequestException, SendSmallRequestException, ServerFullLoadedException, SendBigRequestToReservedException {
        Collection<MyInstance> instances = this.instancesMap.values();
        MyInstance chosen = null;
        double lowest_instance_load = Double.POSITIVE_INFINITY;

        for (MyInstance instance : instances){
            // not healthy or reserved for a big request
            if ((!instance.isHealthy()) || (instance.isReserved())) continue;

            double instance_load = instance.getWorkload();
            if (instance_load < lowest_instance_load) {
                lowest_instance_load = instance_load;
                chosen = instance;
            }
        }

        if (chosen == null) throw new NoInstanceAvailableException();

        double totalLoad = lowest_instance_load + predicted_cost;
        double relativePredictedLoad = predicted_cost/InstanceManager.getMaximumCost();
        double relative_totalLoad = totalLoad/InstanceManager.getMaximumCost();

        if (relative_totalLoad > MAXIMUM_LOAD){
            if (reserve){
                throw new SendBigRequestToReservedException(predicted_cost, relativePredictedLoad, r);
            }
            else throw new ServerFullLoadedException();
        }
        else {
            chosen.setWorkload(totalLoad);
            chosen.setRelative_workload(relative_totalLoad);
            // big request
            if (relativePredictedLoad >= BIG_REQUEST_THRESHOLD) {
                throw new SendBigRequestException(chosen, r);
            }

            // intermediate request
            else if ((relativePredictedLoad < BIG_REQUEST_THRESHOLD) && (relativePredictedLoad >= SMALL_REQUEST_THRESHOLD))
                throw new SendIntermediateRequestException(chosen, r);

            // small request
            else if (relativePredictedLoad < SMALL_REQUEST_THRESHOLD)
                throw  new SendSmallRequestException(chosen, r);
        }
    }

    public synchronized float calculateAverageLoad(){
        Collection<MyInstance> instances = this.instancesMap.values();
        float average_load = 0;
        int healthy_instances = this.getNumHealthyInstances();

        for (MyInstance instance : instances){
            if (!instance.isHealthy()){
                continue;
            }
            double instance_avg_load = instance.getWorkload();
            average_load += instance_avg_load;
        }
        if (healthy_instances != 0) {
            average_load = average_load / healthy_instances;
            average_load = (float) (average_load / getMaximumCost());

        }
        else {
            average_load = 0;
        }

        System.out.println(String.format("MESSAGE: Instances have an Average LOAD utilization of %.1f%%", average_load*100));

        return average_load;
    }

    public synchronized int getNumHealthyInstances(){
        int active = 0;
        Collection<MyInstance> instances = this.instancesMap.values();
        for (MyInstance instance : instances){
            if (instance.isHealthy()) active++;
        }
        return active;
    }

    public List<Map<String, AttributeValue>> queryDynamoDB(){
        return this.aws.queryEntireDynamo();
    }

    public void writeCacheToDynamoDB(ConcurrentHashMap<String, CachedSample> cache){
        for (Map.Entry<String, CachedSample> sample : cache.entrySet()) {
            String primaryKey = sample.getKey();
            CachedSample cachedSample = sample.getValue();

            Measurement m = cachedSample.getMeasurement();
            int range = cachedSample.getRange();

            this.aws.addItemtoDynamo(primaryKey, m.getCost(), m.getRequest().getPuzzle_name(),
                    m.getRequest().getStrategy(), m.getRequest().getSizeX(), m.getRequest().getSizeY(),
                    m.getRequest().getMiss_ele(), range);
        }
    }

//    public synchronized float calculateAverageCPU(){
//        Collection<MyInstance> instances = this.instancesMap.values();
//        float average_CPU = 0;
//        for (MyInstance instance : instances){ //TODO: codigo ser extensivel?
//            // TODO: adicionar caso em que a instancia nao esta healthy
//            Instance AWSinstance = this.aws.getAWSInstance(instance);
//            float instance_avg_CPU = this.aws.getInstanceAverageCPU(AWSinstance);
//            if (instance_avg_CPU == -1){
//                continue;
//            };
//            average_CPU += instance_avg_CPU;
//        }
//        average_CPU = average_CPU / (instances.size());
//
//        if (average_CPU ==Double.NaN){
//            System.out.println("MESSAGE: There weren't found datapoints.");
//            average_CPU = 30;
//        }
//        System.out.println(String.format("MESSAGE: Instances have an Average CPU utilization of %f", average_CPU));
//
//        return average_CPU;
//    }

//    public synchronized MyInstance AvailableLeastLoadedInstance (double predicted_cost) throws NoInstanceAvailableException {
//        Collection<MyInstance> instances = this.instancesMap.values();
//        MyInstance chosen = null;
//        double lowest_instance_load = Double.POSITIVE_INFINITY;
//
//        for (MyInstance instance : instances){
//            if (!instance.isHealthy()) continue;
//            double instance_load = instance.getWorkload();
//            if (instance_load < lowest_instance_load) {
//                lowest_instance_load = instance_load;
//                chosen = instance;
//            }
//        }
//
//        if (chosen == null) throw new NoInstanceAvailableException();
//
//        double totalLoad = lowest_instance_load + predicted_cost;
//        double relative_totalLoad = totalLoad/InstanceManager.getMaximumCost();
//
//        if (relative_totalLoad > MAXIMUM_LOAD){
//            return null;
//        }
//        else {
//            chosen.setWorkload(totalLoad);
//            chosen.setRelative_workload(relative_totalLoad);
//        }
//        return chosen;
//    }
}
