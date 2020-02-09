# Gatling runner Docker image with support for realtime monitoring

## Build and run
```
docker build -t gatling-runner .
docker run --rm --name gatling-runner --network gatling-monitoring_gatling gatling-runner -gh gatling-monitoring_influxdb_1 -gp 2003
```

# Gatling Realtime Monitoring
https://gatling.io/docs/current/realtime_monitoring/


