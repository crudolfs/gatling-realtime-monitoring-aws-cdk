package com.rudolfs.gatling.cdk;

import com.rudolfs.gatling.cdk.ecs.GatlingEcrStack;
import com.rudolfs.gatling.cdk.ecs.GatlingEcsFargateStack;
import com.rudolfs.gatling.cdk.vpc.SharedVpcStack;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Objects;

/**
 * AWS CDK app that creates the stack for a Gatling Realtime Monitoring app.
 */
public class GatlingRealtimeMonitoringCdkApp {
    private static final String VPC_NAME_SHARED = "shared-vpc";
    private static final String ECS_CLUSTER_NAME = "gatling-realtime-monitoring";
    private static final String CLOUDMAP_NAMESPACE = "gatling-realtime-monitoring.com";
    private static final String CLUSTER_NAMESPACE = "gatling";
    private static final String GATLING_RUNNER_SERVICE_NAME = "gatling-runner";
    private static final String GATLING_MONITORING_SERVICE_NAME = "gatling-monitoring";
    private static final String GRAFANA_CONTAINER_NAME = "grafana";
    private static final String INFLUXDB_CONTAINER_NAME = "influxdb";

    public static void main(final String[] args) {
        App app = new App();

        String account = Objects.requireNonNull(System.getenv("CDK_DEFAULT_ACCOUNT"), "CDK_DEFAULT_ACCOUNT is required.");
        String region = Objects.requireNonNull(System.getenv("CDK_DEFAULT_REGION"), "CDK_DEFAULT_REGION is required.");

        StackProps stackProps = StackProps.builder()
                .env(Environment.builder()
                        .account(account)
                        .region(region)
                        .build())
                .build();

        SharedVpcStack.builder().scope(app).id("SharedVpcStack").stackProps(stackProps)
                .vpcName(VPC_NAME_SHARED)
                .build();

        GatlingEcrStack.builder().scope(app).id("GatlingEcrStack").stackProps(stackProps)
                .repositoryNamespace(CLUSTER_NAMESPACE)
                .gatlingRunnerRepositoryName(GATLING_RUNNER_SERVICE_NAME)
                .grafanaRepositoryName(GRAFANA_CONTAINER_NAME)
                .influxDBRepositoryName(INFLUXDB_CONTAINER_NAME)
                .build();

        GatlingEcsFargateStack.builder().scope(app).id("GatlingEcsFargateStack").stackProps(stackProps)
                .cloudMapNamespace(CLOUDMAP_NAMESPACE)
                .ecsClusterName(ECS_CLUSTER_NAME)
                .gatlingRunnerServiceName(GATLING_RUNNER_SERVICE_NAME)
                .gatlingMonitoringServiceName(GATLING_MONITORING_SERVICE_NAME)
                .grafanaContainerName(GRAFANA_CONTAINER_NAME)
                .influxDBContainerName(INFLUXDB_CONTAINER_NAME)
                .clusterNamespace(CLUSTER_NAMESPACE)
                .vpcName(VPC_NAME_SHARED)
                .build();

        app.synth();
    }
}
