Generate the default grafana.ini file:
```
docker build -t grafana .
docker run --name=grafana -d -p 3000:3000 grafana
docker exec -it grafana bash
```

https://grafana.com/docs/grafana/latest/installation/configuration/


```
aws ecs update-service --cluster gatling-monitoring-loadtest --service grafana-influxdb --desired-count 1
```


https://github.com/grafana/grafana/issues/12896
