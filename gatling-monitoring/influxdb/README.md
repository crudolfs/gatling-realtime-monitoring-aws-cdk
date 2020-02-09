# InfluxDB Docker image for Gatling

## Build and run
```
docker build -t influxdb .
docker run --name=influxdb -d -p 8086:8086 influxdb
docker exec -it influxdb influx
```
