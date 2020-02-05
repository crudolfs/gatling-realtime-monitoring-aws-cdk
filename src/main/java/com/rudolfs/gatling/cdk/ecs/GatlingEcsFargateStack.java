package com.rudolfs.gatling.cdk.ecs;

import com.rudolfs.gatling.cdk.StackBuilder;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.servicediscovery.NamespaceType;

/**
 * Creates the CloudFormation for the AWS ECS Cluster and Services for the Gatling Real-Time Monitoring stack.
 */
public class GatlingEcsFargateStack extends Stack {

    private GatlingEcsFargateStack(Builder builder) {
        super(builder.getScope(), builder.getId(), builder.getStackProps());

        // VPC and subnets lookup
        IVpc vpc = Vpc.fromLookup(this, "sharedVpc", VpcLookupOptions.builder()
                .vpcName(builder.vpcName)
                .build());

        // ECS Cluster setup
        Cluster ecsCluster = Cluster.Builder.create(this, "GatlingRealtimeMonitoringLoadTest")
                .clusterName(builder.ecsClusterName)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name(builder.cloudMapNamespace)
                        .type(NamespaceType.DNS_PRIVATE)
                        .vpc(vpc)
                        .build())
                .vpc(vpc)
                .build();

        // IAM Roles needed to execute AWS ECS Fargate tasks
        final Role fargateExecutionRole = new FargateExecutionRole(this, "FargateEcsExecutionRole");
        final Role fargateTaskRole = new FargateTaskRole(this, "FargateEcsTaskRole");

        // Fargate service for Grafana and InfluxDB
        FargateService gatlingMonitoringFargateService = GatlingMonitoringFargateService.builder()
                .serviceDiscoveryName(builder.gatlingMonitoringServiceDiscoveryServiceName)
                .grafanaContainerName("grafanaContainer")
                .influxDBContainerName("influxdbContainer")
                .fargateServiceProps(
                        GatlingFargateServiceProps.builder()
                                .serviceName(builder.gatlingMonitoringServiceName)
                                .clusterNamespace(builder.clusterNamespace)
                                .ecsCluster(ecsCluster)
                                .fargateExecutionRole(fargateExecutionRole)
                                .fargateTaskRole(fargateTaskRole)
                                .stackProps(builder.getStackProps())
                                .vpc(vpc)
                                .build())
                .build(this, "GrafanaInfluxFargateService")
                .getFargateService();

        final String monitoringServiceEndpointHostname = gatlingMonitoringFargateService.getCloudMapService().getServiceName() + "."
                + gatlingMonitoringFargateService.getCloudMapService().getNamespace().getNamespaceName();

        // Fargate service for Gatling runner
        GatlingRunnerFargateService.builder()
                .monitoringServiceDiscoveryEndpoint(monitoringServiceEndpointHostname)
                .gatlingRunnerContainerName("gatlingRunnerContainer")
                .fargateServiceProps(GatlingFargateServiceProps.builder()
                        .serviceName(builder.gatlingRunnerServiceName)
                        .clusterNamespace(builder.clusterNamespace)
                        .ecsCluster(ecsCluster)
                        .fargateExecutionRole(fargateExecutionRole)
                        .fargateTaskRole(fargateTaskRole)
                        .stackProps(builder.getStackProps())
                        .vpc(vpc)
                        .build())
                .build(this, "GatlingRunnerFargateService");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StackBuilder<Builder> {
        private String vpcName;
        private String ecsClusterName;
        private String cloudMapNamespace;
        private String clusterNamespace;
        private String gatlingRunnerServiceName;
        private String gatlingMonitoringServiceName;
        private String gatlingMonitoringServiceDiscoveryServiceName;

        public Builder vpcName(String vpcName) {
            this.vpcName = vpcName;
            return this;
        }

        public Builder ecsClusterName(String ecsClusterName) {
            this.ecsClusterName = ecsClusterName;
            return this;
        }

        public Builder cloudMapNamespace(String cloudMapNamespace) {
            this.cloudMapNamespace = cloudMapNamespace;
            return this;
        }

        public Builder clusterNamespace(String repositoryNamespace) {
            this.clusterNamespace = repositoryNamespace;
            return this;
        }

        public Builder gatlingRunnerServiceName(String serviceName) {
            this.gatlingRunnerServiceName = serviceName;
            return this;
        }

        public Builder gatlingMonitoringServiceName(String serviceName) {
            this.gatlingMonitoringServiceName = serviceName;
            return this;
        }

        public Builder gatlingMonitoringServiceDiscoveryServiceName(String serviceName) {
            this.gatlingMonitoringServiceDiscoveryServiceName = serviceName;
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
