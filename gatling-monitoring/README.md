# Gatling Realtime Monitoring
This module contains the Docker images for the following components:
- [gatling-runner](gatling-runner/README.md)
- [grafana](grafana/README.md)
- [influxdb](influxdb/README.md)

## Build and run
### Local
Build:
`docker-compose build`

Run:
`docker-compose up`

Stop:
`docker-compose down`

### AWS ECS Fargate
Build: build the infra with AWS CDK as explained [here](../aws-cdk/README.md).

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

## Gatling Dashboard UI
Local: open a browser at http://localhost:3000 (default login: admin/admin).
