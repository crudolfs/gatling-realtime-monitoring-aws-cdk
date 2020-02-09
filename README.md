# Gatling Realtime Monitoring in AWS ECS
This repository contains the AWS infrastructure as code and Docker images for a Gatling setup with support for realtime monitoring (as 
described [here](https://gatling.io/docs/current/realtime_monitoring/)).

## Architecture
![architecture](images/gatling-realtime-monitoring-architecture.png "gatling realtime monitoring architecture overview")

## Infrastructure as code
AWS CDK (Cloud Development Kit) is used to generate the CloudFormation and can be found in [aws-cdk](aws-cdk).
See the [README](aws-cdk/README.md) for more information.

## Docker images
The custom docker images for Grafana, InfluxDB and Gatling can be found in [gatling-monitoring](gatling-monitoring).
See the [README](gatling-monitoring/README.md) for more information.


