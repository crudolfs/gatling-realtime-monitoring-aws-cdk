FROM grafana/grafana:5.4.3

COPY provisioning/dashboards/dashboards.yaml /etc/grafana/provisioning/dashboards/dashboards.yaml
COPY provisioning/dashboards/gatling-dashboard-1.json /var/lib/grafana/dashboards/gatling-dashboard-1.json
COPY provisioning/dashboards/gatling-dashboard-2.json /var/lib/grafana/dashboards/gatling-dashboard-2.json
COPY provisioning/dashboards/gatling-dashboard-3.json /var/lib/grafana/dashboards/gatling-dashboard-3.json
COPY provisioning/datasources/influx.yaml /etc/grafana/provisioning/datasources/influx.yaml

ENTRYPOINT [ "/run.sh" ]
