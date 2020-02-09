FROM maven:3.6.1-jdk-8-alpine as build

WORKDIR /usr/build

COPY pom.xml pom.xml
COPY src/main src/main

RUN mvn clean install

FROM openjdk:8-jre-alpine

WORKDIR /usr/app

COPY --from=build /usr/build/target/gatling-runner-1.0-SNAPSHOT.jar ./gatling-runner.jar
COPY ./run.sh ./run.sh

ENTRYPOINT ["./run.sh"]
