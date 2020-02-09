FROM grafana/grafana:5.4.3

USER root

RUN apt-get update \
 && apt-get install -y --no-install-recommends gettext

COPY provisioning/dashboards/dashboards.yaml /etc/grafana/provisioning/dashboards/dashboards.yaml
COPY provisioning/dashboards/gatling-dashboard-1.json /var/lib/grafana/dashboards/gatling-dashboard-1.json
COPY provisioning/dashboards/gatling-dashboard-2.json /var/lib/grafana/dashboards/gatling-dashboard-2.json
COPY provisioning/dashboards/gatling-dashboard-3.json /var/lib/grafana/dashboards/gatling-dashboard-3.json
COPY provisioning/datasources/influx.yaml /etc/grafana/provisioning/datasources/influx.yaml

ENV INFLUXDB_ACCESS_MODE direct
ENV INFLUXDB_HOST localhost
ENV INFLUXDB_PORT 8086

ENTRYPOINT envsubst < /etc/grafana/provisioning/datasources/influx.yaml > conf.yaml && mv conf.yaml /etc/grafana/provisioning/datasources/influx.yaml \
        && su grafana \
        && sh /run.sh
