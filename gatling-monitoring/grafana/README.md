# Grafana Docker image for Gatling 
This directory contains a Docker image for Grafana with a pre-installed configuration for several Gatling 
dashboards and a datasource connection to an InfluxDB time series database.

## Build and run
Use the [docker-compose.yml](../docker-compose.yml) file (in parent directory) to build this image and run the container.
```
docker-compose build
docker-compose up
docker-compose down
```

In order to build and run separate, use the following commands (from this directory):
```
docker build -t grafana .
docker run -e INFLUXDB_HOST=localhost -e INFLUXDB_PORT=8086 -e INFLUXDB_ACCESS_MODE=direct --rm --name=grafana -d -p 3000:3000 grafana
```

The INFLUXDB_HOST, INFLUXDB_PORT and INFLUXDB_ACCESS_MODE environment variables are used in the influx.yaml datasource configuration file.
Default values for these variables are specified in the Dockerfile ('localhost', '8086' and 'direct').

Verify in a browser that the Grafana UI is accessible at http://localhost:3000 (default login: admin/admin).

