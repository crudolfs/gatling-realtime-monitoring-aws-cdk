# Gatling Realtime Monitoring
This folder contains the Docker images for the following components:
- [gatling-runner](gatling-runner/README.md)
- [grafana](grafana/README.md)
- [influxdb](influxdb/README.md)

## Build and run
### Local
#### Gatling-monitoring
The application can run on your local machine in Docker containers. The monitoring part (Grafana and InfluxDB) 
can be built, started and stopped with docker-compose as follows:

Build:
`docker-compose build`

Run:
`docker-compose up`

Stop:
`docker-compose down`

Verify in a browser that the Grafana UI is accessible at http://localhost:3000 (default login: admin/admin).

#### Gatling-runner
The Gatling runner can be built from within the [gatling-runner](gatling-runner) directory as follows:

`docker build -t gatling-runner .`

A Gatling test run can be started as follows: 

`docker run --rm --name gatling-runner --network gatling-monitoring_gatling gatling-runner -gh gatling-monitoring_influxdb_1 -gp 2003`

The --network parameter is required to communicate with InfluxDB which is started via [docker-compose](./docker-compose.yml).
The -gh and -gp options indicate the GATLING_GRAPHITE_HOST and GATLING_GRAPHITE_PORT respectively which is needed to communicate with InfluxDB.


### AWS ECS Fargate
Build: create the AWS infrastructure with AWS CDK as explained in the [aws-cdk README](../aws-cdk/README.md).

Run: 
```
aws ecs update-service --cluster gatling-realtime-monitoring --service gatling-monitoring --desired-count 1
aws ecs update-service --cluster gatling-realtime-monitoring --service gatling-runner --desired-count 1
```

Stop:
```
aws ecs update-service --cluster gatling-realtime-monitoring --service gatling-runner --desired-count 0`
aws ecs update-service --cluster gatling-realtime-monitoring --service gatling-monitoring --desired-count 0`
```
