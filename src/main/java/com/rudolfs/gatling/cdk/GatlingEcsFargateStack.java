package com.rudolfs.gatling.cdk;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.servicediscovery.NamespaceType;

public class GatlingEcsFargateStack extends Stack {
    private static final String VPC_NAME_SHARED = "shared-vpc";

    public GatlingEcsFargateStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        // VPC and subnets lookup
        IVpc vpc = Vpc.fromLookup(this, "sharedVpc", VpcLookupOptions.builder()
                .vpcName(VPC_NAME_SHARED)
                .build());

        // ECS Cluster setup
        Cluster ecsCluster = Cluster.Builder.create(this, "GatlingRealtimeMonitoringLoadTest")
                .clusterName("gatling-monitoring-loadtest")
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("gatling-monitoring.com")
                        .type(NamespaceType.DNS_PRIVATE)
                        .vpc(vpc)
                        .build())
                .vpc(vpc)
                .build();

        // IAM Roles needed to execute AWS ECS Fargate tasks
        final Role fargateExecutionRole = new FargateExecutionRole(this, "FargateEcsExecutionRole");
        final Role fargateTaskRole = new FargateTaskRole(this, "FargateEcsTaskRole");

        // Fargate service for Gatling runner
        new GatlingRunnerFargateService(this, "GatlingRunnerFargateService",
                GatlingFargateServiceProps.builder()
                        .ecsCluster(ecsCluster)
                        .fargateExecutionRole(fargateExecutionRole)
                        .fargateTaskRole(fargateTaskRole)
                        .stackProps(props)
                        .vpc(vpc)
                        .build());

        // Fargate service for Grafana and InfluxDB
        new GatlingMonitoringFargateService(this, "GrafanaInfluxFargateService",
                GatlingFargateServiceProps.builder()
                        .ecsCluster(ecsCluster)
                        .fargateExecutionRole(fargateExecutionRole)
                        .fargateTaskRole(fargateTaskRole)
                        .stackProps(props)
                        .vpc(vpc)
                        .build());
    }
}
