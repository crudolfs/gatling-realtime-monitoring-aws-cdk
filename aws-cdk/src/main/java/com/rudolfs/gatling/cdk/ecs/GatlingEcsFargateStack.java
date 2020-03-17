package com.rudolfs.gatling.cdk.ecs;

import com.rudolfs.gatling.cdk.StackBuilder;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.servicediscovery.NamespaceType;

/**
 * Creates the CloudFormation for the AWS ECS Cluster and Services for the Gatling Real-Time Monitoring stack.
 */
public class GatlingEcsFargateStack extends Stack {
    private static final String DEFAULT_GATLING_RUNNER_SERVICE_NAME = "gatling-runner";
    private static final String DEFAULT_GATLING_DASHBOARD_SERVICE_NAME = "dashboard";

    private GatlingEcsFargateStack(Builder builder) {
        super(builder.getScope(), builder.getId(), builder.getStackProps());

        // VPC and subnets lookup
        IVpc vpc = Vpc.fromLookup(this, "gatlingVpc", VpcLookupOptions.builder()
                .vpcName(builder.vpcName)
                .build());

        // ECS Cluster setup
        Cluster ecsCluster = Cluster.Builder.create(this, "GatlingRealtimeMonitoringLoadTest")
                .clusterName(builder.ecsClusterName)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name(builder.namespace)
                        .type(NamespaceType.DNS_PRIVATE)
                        .vpc(vpc)
                        .build())
                .vpc(vpc)
                .build();

        // IAM Roles needed to execute AWS ECS Fargate tasks
        final Role fargateExecutionRole = new FargateExecutionRole(this, "FargateEcsExecutionRole", builder.namespace);
        final Role fargateTaskRole = new FargateTaskRole(this, "FargateEcsTaskRole", builder.namespace);
        // service discovery endpoint of the gatling dashboard ECS service
        final String dashboardServiceEndpointHostname = DEFAULT_GATLING_DASHBOARD_SERVICE_NAME + "." + builder.namespace;

        // Fargate service for Grafana and InfluxDB
        GatlingDashboardFargateService.builder()
                .serviceDiscoveryName(DEFAULT_GATLING_DASHBOARD_SERVICE_NAME)
                .fargateServiceProps(
                        GatlingFargateServiceProps.builder()
                                .serviceName(DEFAULT_GATLING_DASHBOARD_SERVICE_NAME)
                                .clusterNamespace(builder.namespace)
                                .ecsCluster(ecsCluster)
                                .gatlingEcrProps(builder.gatlingEcrProps)
                                .gatlingDashboardServiceDiscoveryEndpoint(dashboardServiceEndpointHostname)
                                .fargateExecutionRole(fargateExecutionRole)
                                .fargateTaskRole(fargateTaskRole)
                                .stackProps(builder.getStackProps())
                                .vpc(vpc)
                                .build())
                .build(this, "GrafanaInfluxFargateService");

        // Fargate service for Gatling runner
        new GatlingRunnerFargateService(this, "GatlingRunnerFargateService",
                GatlingFargateServiceProps.builder()
                        .serviceName(DEFAULT_GATLING_RUNNER_SERVICE_NAME)
                        .clusterNamespace(builder.namespace)
                        .ecsCluster(ecsCluster)
                        .gatlingEcrProps(builder.gatlingEcrProps)
                        .gatlingDashboardServiceDiscoveryEndpoint(dashboardServiceEndpointHostname)
                        .fargateExecutionRole(fargateExecutionRole)
                        .fargateTaskRole(fargateTaskRole)
                        .stackProps(builder.getStackProps())
                        .vpc(vpc)
                        .build()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StackBuilder<Builder> {
        private String vpcName;
        private String ecsClusterName;
        private String namespace;
        private GatlingEcrProps gatlingEcrProps;

        public Builder vpcName(String vpcName) {
            this.vpcName = vpcName;
            return this;
        }

        public Builder ecsClusterName(String ecsClusterName) {
            this.ecsClusterName = ecsClusterName;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder gatlingEcrProps(GatlingEcrProps gatlingEcrProps) {
            this.gatlingEcrProps = gatlingEcrProps;
            return this;
        }

        public GatlingEcsFargateStack build() {
            return new GatlingEcsFargateStack(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
