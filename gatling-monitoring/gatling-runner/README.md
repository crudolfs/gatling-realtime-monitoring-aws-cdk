```
mvn archetype:generate -DarchetypeGroupId=io.gatling.highcharts -DarchetypeArtifactId=gatling-highcharts-maven-archetype

docker run -it --name gatling-runner --network gatling-monitoring_gatling gatling-runner-fatjar ash

mvn -DGATLING_GRAPHITE_HOST=localhost gatling:test

java -DGATLING_GRAPHITE_HOST=localhost -jar target/gatling-runner-1.0-SNAPSHOT.jar -s simulations.BasicSimulation
java -DGATLING_GRAPHITE_HOST=localhost -jar gatling-runner.jar -s simulations.BasicSimulation

java -DGATLING_GRAPHITE_HOST=gatling-monitoring_influxdb_1 -jar gatling-runner.jar -s simulations.BasicSimulation

```

https://gatling.io/docs/current/realtime_monitoring/

https://www.testingexcellence.com/gatling-maven-performance-test-framework/
https://www.blazemeter.com/blog/how-to-set-up-a-gatling-tests-implementation-environment

```
mvn clean install -Dhost=assetscalingservice-dev-alb-1056189997.eu-west-1.elb.amazonaws.com -Dport=80 -Dbucket=asset-scaling-test -Dprefix=assets gatling:test -DGATLING_GRAPHITE_HOST=localhost
```
