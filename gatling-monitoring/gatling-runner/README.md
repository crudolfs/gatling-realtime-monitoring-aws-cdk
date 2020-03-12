# Gatling runner Docker image with support for realtime monitoring
This directory contains a Docker image for Gatling with pre-installed simulations and a graphite configuration that allows to communicate with an InfluxDB 
time series database. The simulations are used as an example and taken from the 
[gatling-sbt-plugin-demo](https://github.com/gatling/gatling-sbt-plugin-demo/tree/master/src/test/scala/computerdatabase) repository.

## Build and run
The Gatling runner can be built as follows:

`docker build -t gatling-runner .`

A Gatling test run can be started as follows: 

`docker run --rm --name gatling-runner --network gatling-monitoring_gatling gatling-runner -gh gatling-monitoring_influxdb_1 -gp 2003`

The --network parameter is required to communicate with InfluxDB which is started via [docker-compose](../docker-compose.yml).  
The -gh and -gp options indicate the GATLING_GRAPHITE_HOST and GATLING_GRAPHITE_PORT respectively which is needed to communicate with InfluxDB.



