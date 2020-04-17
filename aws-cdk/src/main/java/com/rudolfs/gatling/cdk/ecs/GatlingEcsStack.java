package com.rudolfs.gatling.cdk.ecs;

import com.rudolfs.gatling.cdk.StackBuilder;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.UserData;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.EcsOptimizedImage;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.servicediscovery.NamespaceType;

import java.util.List;

/**
 * Creates the CloudFormation for the AWS ECS Cluster and Services for the Gatling Real-Time Monitoring stack.
 * The Gatling ECS cluster uses Fargate to run stateless services and EC2 to run stateful services (InfluxDB in this case).
 */
public class GatlingEcsStack extends Stack {
    private static final String DEFAULT_GATLING_RUNNER_SERVICE_NAME = "gatling-runner";
    private static final String DEFAULT_GRAFANA_SERVICE_NAME = "grafana";
    private static final String DEFAULT_INFLUXDB_SERVICE_NAME = "influxdb";
    private static final String DEFAULT_AVAILABILITY_ZONE_EBS_VOLUME = "eu-west-1a";

    private GatlingEcsStack(Builder builder) {
        super(builder.getScope(), builder.getId(), builder.getStackProps());

        // VPC and subnets lookup
        IVpc vpc = Vpc.fromLookup(this, "gatlingVpc", VpcLookupOptions.builder()
                .vpcName(builder.vpcName)
                .build());

        // ECS Cluster setup
        Cluster ecsCluster = Cluster.Builder.create(this, "GatlingCluster")
                .clusterName(builder.ecsClusterName)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name(builder.namespace)
                        .type(NamespaceType.DNS_PRIVATE)
                        .vpc(vpc)
                        .build())
                .vpc(vpc)
                .build();

        // Role used for the EC2 service types, this role contains the correct statements to allow Rexray volume driver to do its work
        IRole instanceProfileRole = new Ec2InstanceProfileRole(this, "InstanceProfileRole", builder.namespace);

        // EBS volume is used to store data for stateful services. This can only be re-attached within the
        // AvailabilityZone (AZ) where the EBS volume is created. Therefore determine which AZ to use.
        ISubnet privateSubnet = vpc.getPrivateSubnets().get(0);
        String availabilityZone = privateSubnet != null ? privateSubnet.getAvailabilityZone() : DEFAULT_AVAILABILITY_ZONE_EBS_VOLUME;

        // AutoScalingGroup needed for the stateful (EC2) services
        AutoScalingGroup autoScalingGroup = AutoScalingGroup.Builder.create(this, "AutoScalingGroup")
                // important to install rexray volume driver and restart ECS agent afterwards
                .userData(UserData.custom(String.format("#!/bin/bash\n" +
                                "echo ECS_CLUSTER=%s >> /etc/ecs/ecs.config\n" +
                                "echo ECS_AWSVPC_BLOCK_IMDS=true >> /etc/ecs/ecs.config\n" +
                                "docker plugin install rexray/ebs REXRAY_PREEMPT=true EBS_REGION=%s --grant-all-permissions\n" +
                                "stop ecs\n" +
                                "start ecs"
                        , builder.ecsClusterName, availabilityZone)))
                // use t3a.medium
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.MEDIUM))
                .machineImage(EcsOptimizedImage.amazonLinux2())
                .role(instanceProfileRole)
                // limitation of EBS volume attachment within same AZ requires to allow EC2 instance creation in 1 AZ (and therefore subnet) only
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(List.of(privateSubnet))
                        .build())
                .vpc(vpc)
                .build();

        ecsCluster.addAutoScalingGroup(autoScalingGroup);

        // IAM Roles needed to execute AWS ECS Fargate tasks
        Role fargateExecutionRole = new FargateExecutionRole(this, "FargateEcsExecutionRole", builder.namespace);
        Role fargateTaskRole = new FargateTaskRole(this, "FargateEcsTaskRole", builder.namespace);
        // service discovery endpoint of the InfluxDB service
        String influxdbHostName = DEFAULT_INFLUXDB_SERVICE_NAME + "." + builder.namespace;

        // Fargate service for Grafana
        GrafanaFargateService.builder()
                .influxdbHostName(influxdbHostName)
                .fargateServiceProps(
                        GatlingEcsServiceProps.builder()
                                .serviceName(DEFAULT_GRAFANA_SERVICE_NAME)
                                .clusterNamespace(builder.namespace)
                                .ecsCluster(ecsCluster)
                                .fargateExecutionRole(fargateExecutionRole)
                                .fargateTaskRole(fargateTaskRole)
                                .vpc(vpc)
                                .build()
                ).build(this, "GrafanaFargateService");

        // EC2 (stateful) service for InfluxDB
        new InfluxdbEc2Service(this, "InfluxdbFargateService",
                GatlingEcsServiceProps.builder()
                        .serviceName(DEFAULT_INFLUXDB_SERVICE_NAME)
                        .clusterNamespace(builder.namespace)
                        .ecsCluster(ecsCluster)
                        .fargateExecutionRole(fargateExecutionRole)
                        .fargateTaskRole(fargateTaskRole)
                        .vpc(vpc)
                        .build()
        );

        // Fargate service for Gatling runner
        GatlingRunnerFargateService.builder()
                .influxdbHostName(influxdbHostName)
                .fargateServiceProps(
                        GatlingEcsServiceProps.builder()
                                .serviceName(DEFAULT_GATLING_RUNNER_SERVICE_NAME)
                                .clusterNamespace(builder.namespace)
                                .ecsCluster(ecsCluster)
                                .fargateExecutionRole(fargateExecutionRole)
                                .fargateTaskRole(fargateTaskRole)
                                .vpc(vpc)
                                .build()
                ).build(this, "GatlingRunnerFargateService");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StackBuilder<Builder> {
        private String vpcName;
        private String ecsClusterName;
        private String namespace;

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

        public GatlingEcsStack build() {
            return new GatlingEcsStack(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
