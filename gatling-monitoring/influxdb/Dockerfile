FROM influxdb:1.7-alpine

WORKDIR /app
#COPY entrypoint.sh ./
#RUN chmod u+x entrypoint.sh
COPY influxdb.conf /etc/influxdb/influxdb.conf

ENTRYPOINT ["/entrypoint.sh"]
CMD ["influxd"]
