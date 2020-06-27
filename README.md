The objective of this project was to design an Elastic cluster of web servers that were able to solve sudoku puzzles. The architecture has 4 main components: Load balancer, Auto-Scaler, Metrics Storage System and Worker servers. 

Project grade: 18.7/20

The programming language of the project is Java and it used resources from the Amazon Web Services. Furthermore, it was used the following .jar files: aws-java-sdk-1.11.776 (https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/); httpclient-4.5.12 and httpcore-4.4.13 (from https://hc.apache.org/); BIT tool (http://grupos.ist.utl.pt/meic-cnv/labs/labs-bit/docs/html/packages.html).

Load balancer
This component is the entry point of the system. In other words, it is the component that connects the front-end with the back-end of the architecture. Also, it has the resposability of to redirect requests to the Worker servers in an efficient way. 

Metrics Storage System
This component helps the Load balancer to make a good prediction of the cost of a received request from the client. It is based on storing similar requests in the DynamoDB and doing predictions with these previous requests. Furthermore, it uses a cache for optimization.

Worker servers
These are EC2 instances able to receive requests (an unsolved sudoku puzzle) and solving them. One instance can solve puzzles concurrently. Once a puzzle is finished, it is calculated the cost of solving the puzzle with help of the BIT tool and a cost function implemented. 

Auto-scaler 
This component enables the cluster of servers to be elastic. In other words, on the one hand with an increase traffic of requests it will scale the architecture by adding new worker servers. On the other hand, with a descrease in traffic of reqeuests, the number of worker servers will decrease.

Health checks and Fault tolerance
The system is fault tolerant. Simply put, when a instance is solving different puzzles and goes down, the system is able to redirect requests to other running instances in a transparent way to the client. 
Health checks are sent from the load balancer to the running EC2 instances to check if they are healthy for receiving requests. 