package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryAttributes;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;

import java.util.HashMap;
import java.util.Map;

public class GatlingMonitoringFargateService extends Construct {

    public GatlingMonitoringFargateService(Construct scope, String id, Builder builder) {
        super(scope, id);

        SecurityGroup securityGroup = new SecurityGroup(this, "GrafanaInfluxSecurityGroup", SecurityGroupProps.builder()
                .vpc(builder.serviceProps.getVpc())
                .description(String.format("%s security group", builder.serviceProps.getServiceName()))
                .build());
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3000), "The default port that runs the Grafana UI.");
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8086), "The default port that runs the InfluxDB HTTP service.");
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(2003), "The default port that runs the Graphite service.");

        FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GrafanaInfluxTaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .executionRole(builder.serviceProps.getFargateExecutionRole())
                .taskRole(builder.serviceProps.getFargateTaskRole())
                .build();

        ContainerDefinitionOptions grafanaContainerDefinitionOptions = new GrafanaContainerOptions(this, "GrafanaContainerOptions", builder)
                .getContainerDefinitionOptions();
        fargateTaskDefinition.addContainer(builder.grafanaContainerName, grafanaContainerDefinitionOptions);

        ContainerDefinitionOptions influxContainerDefinitionOptions = new InfluxContainerOptions(this, "InfluxDBContainerOptions", builder)
                .getContainerDefinitionOptions();
        fargateTaskDefinition.addContainer(builder.influxDBContainerName, influxContainerDefinitionOptions);

        FargateService.Builder.create(this, id)
                .serviceName(builder.serviceProps.getServiceName())
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(0)
                .cloudMapOptions(CloudMapOptions.builder()
                        .cloudMapNamespace(builder.serviceProps.getEcsCluster().getDefaultCloudMapNamespace())
                        .dnsRecordType(DnsRecordType.A)
                        .name(builder.serviceDiscoveryName)
                        .build())
                .cluster(builder.serviceProps.getEcsCluster())
                .securityGroup(securityGroup)
                .assignPublicIp(true)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(builder.serviceProps.getVpc().getPublicSubnets())
                        .build())
                .build();
    }

    static class GrafanaContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public GrafanaContainerOptions(Construct scope, String id, Builder builder) {
            super(scope, id);

            Map<String, String> environmentVariables = new HashMap<>();
            environmentVariables.put("INFLUXDB_ACCESS_MODE", "direct");
            environmentVariables.put("INFLUXDB_HOST", "localhost");
            environmentVariables.put("INFLUXDB_PORT", "8086");

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromEcrRepository(
                            Repository.fromRepositoryAttributes(this,
                                    "GatlingGrafanaRepository",
                                    repositoryAttributes(String.format("%s/%s", builder.serviceProps.getClusterNamespace(), builder.grafanaContainerName),
                                            builder.serviceProps.getStackProps().getEnv().getRegion(),
                                            builder.serviceProps.getStackProps().getEnv().getAccount()))))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "grafanaFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s/%s", builder.serviceProps.getClusterNamespace(), builder.grafanaContainerName))
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .build())
                            .streamPrefix(builder.grafanaContainerName)
                            .build()))
                    .environment(environmentVariables)
                    .build();
        }

        public ContainerDefinitionOptions getContainerDefinitionOptions() {
            return this.containerDefinitionOptions;
        }
    }

    static class InfluxContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public InfluxContainerOptions(Construct scope, String id, Builder builder) {
            super(scope, id);

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromEcrRepository(
                            Repository.fromRepositoryAttributes(this,
                                    "GatlingInfluxDBRepository",
                                    repositoryAttributes(String.format("%s/%s", builder.serviceProps.getClusterNamespace(), builder.influxDBContainerName),
                                            builder.serviceProps.getStackProps().getEnv().getRegion(),
                                            builder.serviceProps.getStackProps().getEnv().getAccount()))))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "influxdbFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s/%s", builder.serviceProps.getClusterNamespace(), builder.influxDBContainerName))
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .build())
                            .streamPrefix(builder.influxDBContainerName)
                            .build()))
                    .build();
        }

        public ContainerDefinitionOptions getContainerDefinitionOptions() {
            return this.containerDefinitionOptions;
        }
    }

    private static RepositoryAttributes repositoryAttributes(String repositoryName, String region, String accountId) {
        return RepositoryAttributes.builder()
                .repositoryArn(String.format("arn:aws:ecr:%s:%s:repository/%s", region, accountId, repositoryName))
                .repositoryName(repositoryName)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private GatlingFargateServiceProps serviceProps;
        private String serviceDiscoveryName;
        private String grafanaContainerName;
        private String influxDBContainerName;

        public Builder fargateServiceProps(GatlingFargateServiceProps props) {
            this.serviceProps = props;
            return this;
        }

        public Builder serviceDiscoveryName(String serviceDiscoveryName) {
            this.serviceDiscoveryName = serviceDiscoveryName;
            return this;
        }

        public Builder grafanaContainerName(String grafanaContainerName) {
            this.grafanaContainerName = grafanaContainerName;
            return this;
        }

        public Builder influxDBContainerName(String influxDBContainerName) {
            this.influxDBContainerName = influxDBContainerName;
            return this;
        }

        public GatlingMonitoringFargateService build(Construct scope, String id) {
            return new GatlingMonitoringFargateService(scope, id, this);
        }
    }
}
