package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.xspec.M;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.*;

public class AWSManager {

    private final AmazonEC2 ec2Client;
    private AmazonDynamoDB dynamoDB;
    private AmazonCloudWatch cloudwatch;

    private final String tablename = "Sudoku-Database";
    private final String name = "Sudoku-Instance";
    private final String region = "us-east-1";
    private final String instanceType = "t2.micro";
    private final String imageID = "ami-0b952bc8937c4bfc1";
    private final String securityGroup = "CNV-ssh+http";
    private final String keyName = "CNV-Sudoku-AWS";
    private final int PORT = 8000;

    private final long read_capacity = 1L;
    private final long write_capacity = 1L;

//    private final int DATAPOINT_PERIOD = 30; // TODO: does not work
//    private final int CPU_window = 3 * 60 * 1000; // last 4 minutes (in ms)

    public AWSManager() {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();

        try {
            AWSCredentials credentials = credentialsProvider.getCredentials();
            ec2Client = AmazonEC2ClientBuilder.standard()
                    .withRegion(this.region)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();


            dynamoDB = AmazonDynamoDBClientBuilder.standard()
                    .withCredentials(credentialsProvider)
                    .withRegion(this.region)
                    .build();

//            cloudwatch = AmazonCloudWatchClientBuilder.standard()
//                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
//                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
//                            "https://monitoring.us-east-1.amazonaws.com",
//                            this.region))
//                    .build();;

//            AmazonCloudWatchConfig config = new AmazonCloudWatchConfig();
//            config.ServiceURL = "https://eu-west-1.monitoring.amazonaws.com";


        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
    }

    public Instance startNewInstance() {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId(this.imageID)
                .withInstanceType(this.instanceType)
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(this.keyName)
                .withSecurityGroups(this.securityGroup)
                .withMonitoring(true);

        RunInstancesResult runInstancesResult = this.ec2Client.runInstances(runInstancesRequest);

        Instance newInstance = runInstancesResult.getReservation().getInstances().get(0);

        return newInstance;
    }

    public void removeInstance(String ID){
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(ID);
        ec2Client.terminateInstances(termInstanceReq);
    }

    public List<Instance> getAllInstances() {

        List<Instance> instances = new ArrayList<>(); // by me

        try {
            DescribeAvailabilityZonesResult availabilityZonesResult = this.ec2Client.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                    " Availability Zones.");

            DescribeInstancesResult describeInstancesRequest = this.ec2Client.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            //Set<Instance> instances = new HashSet<Instance>(); // by prof

            for (Reservation reservation : reservations) {
                // by me
                for (Instance instance : reservation.getInstances()) {
                    if (instance.getState().getName().equals("running") && instance.getTags().size() == 0) {
                        System.out.print("\n");
                        System.out.println("ID: " + instance.getInstanceId() + " | DNS: " + instance.getPublicDnsName());
                        instances.add(instance);
                    }

                }
            }
            System.out.print("\n");
            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");

            return instances;
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
        return instances;
    }

    public Map<String, MyInstance> setIPInstance(Map<String,MyInstance> instances, String id){
        DescribeInstancesRequest request = new DescribeInstancesRequest();

        DescribeInstancesResult result = ec2Client.describeInstances(request);
        List<Reservation> reservations = result.getReservations();

        System.out.println("MESSAGE: Setting up instance "+id+" DNS address...");
        for (Reservation reservation : reservations) {
            List<Instance> instances1 = reservation.getInstances();
            for (Instance instance : instances1) {
//                if (instance.getState().getName().equals("running") && instance.getTags().size() == 0) {
//                    String id = instance.getInstanceId();
//                    String ip = instance.getPublicDnsName();
                    //                System.out.println(String.format("Instance ID: %s ; DNS: %s", id, ip));
//                    if (instances.containsKey(id)) {
                    if (instance.getInstanceId().equals(id)){
                        String ip = instance.getPublicDnsName();
                        System.out.println(String.format("Found an instance in manager. ID: %s; DNS: %s", id, ip));
                        MyInstance myInstance = instances.get(id);
                        myInstance.setIp(ip);
                        instances.put(id, myInstance);
                    }
                }
        }

        System.out.println("Instances hashmap");
        for (String name : instances.keySet()) {
            String key = name;
            String value = instances.get(name).getIp();
            System.out.println(key + " " + value);
        }
        System.out.print("\n");

        System.out.println(String.format("MESSAGE: After setting IP addresses, there are %d with correct DNS.", instances.size()));
        System.out.print("\n\n");
        return instances;
    }

    public Map<String, MyInstance> setIPInstances(Map<String, MyInstance> instances) {

        DescribeInstancesRequest request = new DescribeInstancesRequest();

        DescribeInstancesResult result = ec2Client.describeInstances(request);
        List<Reservation> reservations = result.getReservations();

//        System.out.println("Instances hashmap");
//        for (String name : instances.keySet()) {
//            String key = name.toString();
//            String value = instances.get(name).getIp();
//            System.out.println(key + " " + value);
//        }
//        System.out.print("\n");

        System.out.println("MESSAGE: Setting up instances DNS address...");
        for (Reservation reservation : reservations) {
            List<Instance> instances1 = reservation.getInstances();
            for (Instance instance : instances1) {
                if (instance.getState().getName().equals("running") && instance.getTags().size() == 0) {
                    String id = instance.getInstanceId();
                    String ip = instance.getPublicDnsName();
                    //                System.out.println(String.format("Instance ID: %s ; DNS: %s", id, ip));
                    if (instances.containsKey(id)) {
                        System.out.println(String.format("Found an instance in manager. ID: %s; DNS: %s", id, ip));
                        MyInstance myInstance = instances.get(id);
                        myInstance.setIp(ip);
                        instances.put(id, myInstance);
                    }
                }
            }
        }

//        System.out.println("After inserting IP ... Instances in hashmap");
//        for (String name : instances.keySet()) {
//            String key = name.toString();
//            String value = instances.get(name).getIp();
//            System.out.println(key + " " + value);
//        }
//        System.out.println("\n");
        System.out.println(String.format("MESSAGE: After setting IP addresses, there are %d with correct DNS.", instances.size()));
        System.out.print("\n\n");
        return instances;
    }

    public void initDynamoTable() {
        try {
            System.out.println(String.format("Checking if %s has been created already...", tablename));
            System.out.println(String.format("%s is up to date.", tablename));
            TableDescription table_info = dynamoDB.describeTable(tablename).getTable();
            System.out.println("Table Description: " + table_info);

        } catch (AmazonServiceException e) {
            try {
                System.out.println(String.format("%s does not exist.", tablename));
                System.err.println(e.getErrorMessage());
                System.out.println(String.format("Creating %s table ...", tablename));

                // Create a table with a primary hash key named 'name', which holds a string
                CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tablename)
                        .withKeySchema(new KeySchemaElement().withAttributeName("INDEX").withKeyType(KeyType.HASH))
                        .withAttributeDefinitions(new AttributeDefinition().withAttributeName("INDEX").withAttributeType(ScalarAttributeType.S))
                        .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(read_capacity).withWriteCapacityUnits(write_capacity));

                // Create table if it does not exist yet
                TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
                // wait for the table to move into ACTIVE state
                TableUtils.waitUntilActive(dynamoDB, tablename);

                // Describe our new table
                DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tablename);
                TableDescription table_info = dynamoDB.describeTable(describeTableRequest).getTable();
                System.out.println("Table Description: " + table_info);

            } catch (AmazonServiceException ase) {
                System.out.println("Caught an AmazonServiceException, which means your request made it "
                        + "to AWS, but was rejected with an error response for some reason.");
                System.out.println("Error Message:    " + ase.getMessage());
                System.out.println("HTTP Status Code: " + ase.getStatusCode());
                System.out.println("AWS Error Code:   " + ase.getErrorCode());
                System.out.println("Error Type:       " + ase.getErrorType());
                System.out.println("Request ID:       " + ase.getRequestId());
            } catch (AmazonClientException ace) {
                System.out.println("Caught an AmazonClientException, which means the client encountered "
                        + "a serious internal problem while trying to communicate with AWS, "
                        + "such as not being able to access the network.");
                System.out.println("Error Message: " + ace.getMessage());
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                System.out.println(String.format("Error while waiting until active when creating the %s", tablename));
            }
        }
    }

    public void addItemtoDynamo(String primaryKey, double cost, String puzzle, String strategy,
                        double sizeX, double sizeY,int miss_ele, int range){
        try {
            // Add an item
            Map<String, AttributeValue> item = newItem(primaryKey, cost, puzzle, strategy,
                    sizeX, sizeY, miss_ele, range);

            PutItemRequest putItemRequest = new PutItemRequest(tablename, item);
            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
    private static Map<String, AttributeValue> newItem(String primaryKey, double cost, String puzzle, String strategy,
                                                       double sizeX, double sizeY, int miss_ele, int range) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("INDEX", new AttributeValue(primaryKey));
        item.put("cost", new AttributeValue().withN(Integer.toString((int)cost)));
        item.put("puzzle name", new AttributeValue(puzzle));
        item.put("strategy", new AttributeValue(strategy));
        item.put("sizeX", new AttributeValue().withN(Double.toString(sizeX)));
        item.put("sizeY", new AttributeValue().withN(Double.toString(sizeY)));
        item.put("missing elements", new AttributeValue().withN(Integer.toString(miss_ele)));
        item.put("range", new AttributeValue().withN(Integer.toString(range)));

        return item;
    }

    public List<Map<String, AttributeValue>> queryEntireDynamo(){
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(tablename);

        ScanResult result = dynamoDB.scan(scanRequest);
        List<Map<String, AttributeValue>> items = result.getItems();
        return items;
    }

    public synchronized Instance getAWSInstance(MyInstance myInstance){
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.withInstanceIds(myInstance.getId());

        DescribeInstancesResult result = ec2Client.describeInstances(request);
        Reservation reservation = result.getReservations().get(0);

        return reservation.getInstances().get(0);
    }

//    public float getInstanceAverageCPU(Instance instance){
//        float cpu_average = 0;
//
//        try{
//            if (instance.getState().getName().equals("running") && instance.getTags().size() == 0) {
//                System.out.println("Date of start time:" +new Date(new Date().getTime() - CPU_window).toString());
//                System.out.println("Date of end time:" +new Date().toString());
//                GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
//                        .withStartTime(new Date(new Date().getTime() - CPU_window))
//                        .withNamespace("AWS/EC2")
//                        .withPeriod(DATAPOINT_PERIOD)
//                        .withMetricName("CPUUtilization")
//                        .withStatistics("Average")
//                        .withDimensions(new Dimension().withName("InstanceId").withValue(instance.getInstanceId())) // TODO: what is this?
//                        .withEndTime(new Date());
//
//                System.out.println("GetMetricsStatisticsRequest: "+request.toString());
//
//                GetMetricStatisticsResult getMetricStatisticsResult = cloudwatch.getMetricStatistics(request);
//                System.out.println("GetMatricsStatisticsResult: " + getMetricStatisticsResult.toString());
//                List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();
//
//                System.out.println(String.format("Instance %s - #datapoint values: %d",
//                        instance.getInstanceId(), datapoints.size()));
//
//                if (datapoints.size() == 0){
//                    return -1;
//                }
//
//                for (Datapoint dp : datapoints) {
//                    System.out.println(String.format("Instance %s - #datapoint value: %s",
//                            instance.getInstanceId(), dp.toString()));
//                    cpu_average += dp.getAverage();
//                }
//                cpu_average = cpu_average / datapoints.size();
//
//                System.out.println(String.format("Instance %s has an average CPU of: %f", instance.getInstanceId(), cpu_average));
//
//                return cpu_average;
//            }
//        } catch (AmazonServiceException ase) {
//            System.out.println(String.format("Error in AWS manager while calculating average CPU for instance %s",
//                    instance.getInstanceId()));
//            System.out.println("Caught Exception: " + ase.getMessage());
//            System.out.println("Reponse Status Code: " + ase.getStatusCode());
//            System.out.println("Error Code: " + ase.getErrorCode());
//            System.out.println("Request ID: " + ase.getRequestId());
//        }
//
//        return cpu_average;
//    }
}
