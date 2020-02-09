# Grafana Docker image for Gatling 

## Docker
```
docker build -t grafana .
docker run -e INFLUXDB_HOST=localhost -e INFLUXDB_PORT=8086 -e INFLUXDB_ACCESS_MODE=direct --rm --name=grafana -d -p 3000:3000 grafana
docker exec -it grafana bash
```

