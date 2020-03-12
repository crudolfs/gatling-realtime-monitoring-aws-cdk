# InfluxDB Docker image for Gatling
This directory contains a Docker image for InfluxDB that is preconfigured for use with Gatling. The required Graphite templates are specified 
in the influxdb.conf file.

## Build and run
Use the [docker-compose.yml](../docker-compose.yml) file (in parent directory) to build this image and run the container.
```
docker-compose build
docker-compose up
docker-compose down
```

In order to build and run separate, use the following commands (from this directory):
```
docker build -t influxdb .
docker run --name=influxdb -d -p 8086:8086 influxdb
```
