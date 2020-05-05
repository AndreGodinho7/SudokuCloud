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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceManager {
    private static InstanceManager instance;

    private Map<String, MyInstance> instancesMap = new ConcurrentHashMap<>();

    private AWSManager aws;

    private final int PORT = 8000;

    private final int MIN_INSTANCES = 1;

    private InstanceManager() {
        System.out.println("Creating InstanceManager\n");
        System.out.println("Creating AWSManager\n");
        this.aws = new AWSManager();
        System.out.println("Getting all available online EC2 instances\n");
        List<Instance> instances = this.aws.getAllInstances();

        if(instances.size() < this.MIN_INSTANCES) {
            for(int i = 0; i < this.MIN_INSTANCES - instances.size(); i++) {
                this.startNewInstance();
            }
        }
    }

    public static InstanceManager getInstance() {
        if(instance == null) {
            System.out.println("There is no InstanceManager. Creating one ... \n");
            instance = new InstanceManager();
        }

        return instance;
    }

    public synchronized void registerInstance(Instance EC2Instance) {
// file:///home/andregodinho06/Documents/IST/MECD0102/CNV/cnv/aws-java-sdk-1.11.774/documentation/com/amazonaws/services/ec2/model/Instance.html
        String id = EC2Instance.getInstanceId();
        String ip = EC2Instance.getPublicIpAddress();
        System.out.println(String.format("Registering instance %s with ip %s \n", id, ip));
        MyInstance newInstance = new MyInstance(id, ip, this.PORT);

        this.instancesMap.put(id, newInstance);
    }

    public synchronized void startNewInstance() {
        Instance newInstance = this.aws.startNewInstance();
        try {
            Thread.sleep(9000);
            this.registerInstance(newInstance);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
