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

import java.util.ArrayList;

public class AWSManager {

    private final AmazonEC2 ec2Client;

    private final String name = "Sudoku-Instance";

    private final String region = "us-east-1";

    private final String instanceType = "t2.micro";

    private final String imageID = "ami-"; // TODO: define it

    private final String securityGroup = "CNV-ssh+http";

    private final String keyName = "";

    public AWSManager() {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            //init ec2 client
            ec2Client = AmazonEC2ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(this.region)
                    .build();
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

        Tag type = new Tag("Type", "Worker");

        TagSpecification tagSpec = new TagSpecification().withTags(type);

        runInstancesRequest.withImageId(this.imageID)
                .withInstanceType(this.instanceType)
                .withMinCount(1)
                .withMaxCount(1)
                .withTagSpecifications(tagSpec)
                .withKeyName(this.keyName)
                .withSecurityGroups(this.securityGroup)
                .withMonitoring(true);

        RunInstancesResult runInstancesResult = this.ec2Client.runInstances(runInstancesRequest);

        Instance newInstance = runInstancesResult.getReservation().getInstances().get(0);

        return newInstance;
    }

    public List<Instance> getAllInstances() {

        List<Instance> instances = new ArrayList<>(); // by me

        try {
            DescribeAvailabilityZonesResult availabilityZonesResult = this.ec2Client.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                    " Availability Zones.");
            /* using AWS Ireland.
             * TODO: Pick the zone where you have your AMI, sec group and keys */
            DescribeInstancesResult describeInstancesRequest = this.ec2Client.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            //Set<Instance> instances = new HashSet<Instance>(); // by prof

            for (Reservation reservation : reservations) {
                //instances.addAll(reservation.getInstances()); by prof

                // by me
                for(Instance instance : reservation.getInstances()) {
                    if(instance.getState().getName().equals("running") && instance.getTags().get(0).getValue().equals("Worker")) {
                        instances.add(instance);
                    }
                }
            }

            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
            return instances;
        } catch(AmazonServiceException ase) {
        System.out.println("Caught Exception: " + ase.getMessage());
        System.out.println("Reponse Status Code: " + ase.getStatusCode());
        System.out.println("Error Code: " + ase.getErrorCode());
        System.out.println("Request ID: " + ase.getRequestId());
    }
		return instances;
    }
}
