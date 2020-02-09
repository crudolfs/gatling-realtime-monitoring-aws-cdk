FROM influxdb:1.7-alpine

WORKDIR /app
COPY influxdb.conf /etc/influxdb/influxdb.conf

ENTRYPOINT ["/entrypoint.sh"]
CMD ["influxd"]
