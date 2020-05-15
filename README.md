# Gatling Realtime Monitoring in AWS ECS
This repository contains the AWS infrastructure as code and Docker images for a Gatling setup with support for realtime monitoring (as 
described [here](https://gatling.io/docs/current/realtime_monitoring/)).

## Service architecture
The service architecture for this solution is as follows:

![architecture](doc/images/gatling-realtime-monitoring-service-architecture.png "Service architecture for Gatling realtime monitoring.")

## Infrastructure as code
The infrastructure is written in code by leveraging the AWS CDK (Cloud Development Kit). 
The Gatling solution consists of three services that will each run inside a Docker container. These Docker containers 
will be managed by [AWS ECS](https://aws.amazon.com/ecs/) and run in a hybrid (EC2 and Fargate) ECS Cluster.
The AWS CDK cloud components can be found in the [aws-cdk](./aws-cdk) section.

## Docker images
The custom docker images for Grafana, InfluxDB and Gatling can be found in the [gatling-monitoring](./gatling-monitoring) section.


