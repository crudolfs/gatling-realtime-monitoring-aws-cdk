# Gatling Realtime Monitoring
This folder contains the Docker images for the following components:
- [gatling-runner](./gatling-runner)
- [grafana](./grafana)
- [influxdb](./influxdb)

## Local build and deployment
### Gatling-monitoring
The application can run on your local machine in Docker containers. The monitoring part (Grafana and InfluxDB) 
can be built, started and stopped with docker-compose as follows:

Build:
`docker-compose build`

Run:
`docker-compose up`

Stop:
`docker-compose down`

Verify in a browser that the Grafana UI is accessible at http://localhost:3000 (default login: admin/admin).

### Gatling-runner
The Gatling runner can be built from within the [gatling-runner](./gatling-runner) directory as follows:

`docker build -t gatling-runner .`

A Gatling test run can be started as follows: 

`docker run --rm --name gatling-runner --network gatling-monitoring_gatling gatling-runner -gh gatling-monitoring_influxdb_1 -gp 2003`

The --network parameter is required to communicate with InfluxDB which is started via [docker-compose](./docker-compose.yml).
The -gh and -gp options indicate the GATLING_GRAPHITE_HOST and GATLING_GRAPHITE_PORT respectively which is needed to communicate with InfluxDB.

## AWS ECS (hybrid: EC2/Fargate) build and deployment
The AWS infrastructure that is needed to run the Gatling solution in the AWS cloud is available in code in the [aws-cdk](../aws-cdk) folder.
Deploying the gatling AWS CDK app results in creating all AWS infrastructure resources required for this solution. By using the asset 
feature of the AWS CDK this app even builds the local Docker images for the gatling-runner, grafana and influxdb services and pushes these 
to the Docker registry (ECR).  

### Starting and stopping services
The services can be started and stopped with the AWS CLI as follows: 

Start:
```
aws ecs update-service --cluster gatling-cluster --service influxdb --desired-count 1
aws ecs update-service --cluster gatling-cluster --service grafana --desired-count 1
aws ecs update-service --cluster gatling-cluster --service gatling-runner --desired-count 1
```

Stop:
```
aws ecs update-service --cluster gatling-cluster --service gatling-runner --desired-count 0`
aws ecs update-service --cluster gatling-cluster --service influxdb --desired-count 0`
aws ecs update-service --cluster gatling-cluster --service grafana --desired-count 0`
```

When all services are up and running, you can see the realtime performance test results in the Grafana dashboard.
The Grafana dashboard is accessible in a browser at http://<public-ip>:3000 (default login: admin/admin).
The public IP address of Grafana can be found in the AWS Console by opening the Network section of the running 
ECS Task within the dashboard ECS Service. Of course, using a load balancer with a proper DNS would have been more 
appropriate, but is currently not yet part of the infrastructure....  
