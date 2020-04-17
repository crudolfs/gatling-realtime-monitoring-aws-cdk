package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.CloudMapOptions;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.DockerVolumeConfiguration;
import software.amazon.awscdk.services.ecs.Ec2Service;
import software.amazon.awscdk.services.ecs.Ec2TaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.MountPoint;
import software.amazon.awscdk.services.ecs.NetworkMode;
import software.amazon.awscdk.services.ecs.Scope;
import software.amazon.awscdk.services.ecs.Volume;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;

import java.util.List;
import java.util.Map;

public class InfluxdbEc2Service extends Construct {
    private static final String INFLUXDB_DATA_VOLUME_NAME = "influxdb-data";
    private static final String INFLUXDB_DATA_VOLUME_CONTAINER_PATH = "/var/lib/influxdb";

    public InfluxdbEc2Service(Construct scope, String id, GatlingEcsServiceProps serviceProps) {
        super(scope, id);

        SecurityGroup securityGroup = new SecurityGroup(this, "InfluxdbSecurityGroup", SecurityGroupProps.builder()
                .vpc(serviceProps.getVpc())
                .description(String.format("%s security group", serviceProps.getServiceName()))
                .build());
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8086), "The default port that runs the InfluxDB HTTP service.");
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(2003), "The default port that runs the Graphite service.");

        // use RexRay (Docker) volume driver to orchestrate EBS volume attachment to EC2 instances in the cluster
        Volume dataVolume = Volume.builder()
                .name(dataVolumeName(serviceProps.getClusterNamespace()))
                .dockerVolumeConfiguration(DockerVolumeConfiguration.builder()
                        .autoprovision(true)
                        .driver("rexray/ebs")
                        .driverOpts(Map.of("volumetype", "gp2", "size", "20"))
                        .scope(Scope.SHARED)
                        .build())
                .build();

        Ec2TaskDefinition taskDefinition = Ec2TaskDefinition.Builder.create(this, "InfluxdbTaskDefinition")
                .executionRole(serviceProps.getFargateExecutionRole())
                .taskRole(serviceProps.getFargateTaskRole())
                .networkMode(NetworkMode.AWS_VPC)
                .volumes(List.of(dataVolume))
                .build();

        ContainerDefinitionOptions influxContainerDefinitionOptions = new InfluxContainerOptions(this, "InfluxdbContainerOptions", serviceProps)
                .getContainerDefinitionOptions();
        taskDefinition.addContainer(serviceProps.getServiceName(), influxContainerDefinitionOptions)
                .addMountPoints(MountPoint.builder()
                        .sourceVolume(dataVolumeName(serviceProps.getClusterNamespace()))
                        .containerPath(INFLUXDB_DATA_VOLUME_CONTAINER_PATH)
                        .readOnly(false)
                        .build());

        Ec2Service.Builder.create(this, id)
                .serviceName(serviceProps.getServiceName())
                .taskDefinition(taskDefinition)
                .desiredCount(0)
                .cloudMapOptions(CloudMapOptions.builder()
                        .cloudMapNamespace(serviceProps.getEcsCluster().getDefaultCloudMapNamespace())
                        .dnsRecordType(DnsRecordType.A)
                        .name(serviceProps.getServiceName())
                        .build())
                .cluster(serviceProps.getEcsCluster())
                .securityGroup(securityGroup)
                .build();
    }

    private String dataVolumeName(String namespace) {
        return String.format("%s-%s", namespace, INFLUXDB_DATA_VOLUME_NAME);
    }

    static class InfluxContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public InfluxContainerOptions(Construct scope, String id, GatlingEcsServiceProps serviceProps) {
            super(scope, id);

            DockerImageAsset influxdbAsset = DockerImageAsset.Builder.create(this, "influxdbAsset")
                    .directory("../gatling-monitoring/influxdb")
                    .build();

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromDockerImageAsset(influxdbAsset))
                    .memoryReservationMiB(3896)
                    .cpu(2048)
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "influxdbFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s/%s", serviceProps.getClusterNamespace(), serviceProps.getServiceName()))
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .removalPolicy(RemovalPolicy.DESTROY)
                                    .build())
                            .streamPrefix(serviceProps.getServiceName())
                            .build()))
                    .build();
        }

        public ContainerDefinitionOptions getContainerDefinitionOptions() {
            return this.containerDefinitionOptions;
        }
    }
}
