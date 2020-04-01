package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.CloudMapOptions;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;

import java.util.HashMap;
import java.util.Map;

public class GatlingDashboardFargateService extends Construct {

    public GatlingDashboardFargateService(Construct scope, String id, Builder builder) {
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

        final GatlingEcrProps gatlingEcrProps = builder.serviceProps.getGatlingEcrProps();

        ContainerDefinitionOptions grafanaContainerDefinitionOptions = new GrafanaContainerOptions(this, "GrafanaContainerOptions", builder)
                .getContainerDefinitionOptions();
        fargateTaskDefinition.addContainer(gatlingEcrProps.getGrafanaRepositoryName(), grafanaContainerDefinitionOptions);

        ContainerDefinitionOptions influxContainerDefinitionOptions = new InfluxContainerOptions(this, "InfluxDBContainerOptions", builder)
                .getContainerDefinitionOptions();
        fargateTaskDefinition.addContainer(gatlingEcrProps.getInfluxDBRepositoryName(), influxContainerDefinitionOptions);

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

        public GrafanaContainerOptions(Construct scope, String id, GatlingDashboardFargateService.Builder builder) {
            super(scope, id);

            Map<String, String> environmentVariables = new HashMap<>();
            environmentVariables.put("INFLUXDB_ACCESS_MODE", "proxy");
            environmentVariables.put("INFLUXDB_HOST", "localhost");
            environmentVariables.put("INFLUXDB_PORT", "8086");

            final GatlingEcrProps gatlingEcrProps = builder.serviceProps.getGatlingEcrProps();

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromEcrRepository(
                            Repository.fromRepositoryAttributes(this,
                                    "GatlingGrafanaRepository",
                                    gatlingEcrProps.getRepositoryAttributes(
                                            builder.serviceProps.getStackProps().getEnv().getRegion(),
                                            builder.serviceProps.getStackProps().getEnv().getAccount(),
                                            gatlingEcrProps.getGrafanaRepositoryName()
                                    ))))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "grafanaFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s", gatlingEcrProps.getRepositoryNameWithNamespace(gatlingEcrProps.getGrafanaRepositoryName())))
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .build())
                            .streamPrefix(gatlingEcrProps.getGrafanaRepositoryName())
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

        public InfluxContainerOptions(Construct scope, String id, GatlingDashboardFargateService.Builder builder) {
            super(scope, id);

            final GatlingEcrProps gatlingEcrProps = builder.serviceProps.getGatlingEcrProps();

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromEcrRepository(
                            Repository.fromRepositoryAttributes(this,
                                    "GatlingInfluxDBRepository",
                                    gatlingEcrProps.getRepositoryAttributes(
                                            builder.serviceProps.getStackProps().getEnv().getRegion(),
                                            builder.serviceProps.getStackProps().getEnv().getAccount(),
                                            gatlingEcrProps.getInfluxDBRepositoryName()
                                    ))))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "influxdbFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s", gatlingEcrProps.getRepositoryNameWithNamespace(gatlingEcrProps.getInfluxDBRepositoryName())))
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .build())
                            .streamPrefix(gatlingEcrProps.getInfluxDBRepositoryName())
                            .build()))
                    .build();
        }

        public ContainerDefinitionOptions getContainerDefinitionOptions() {
            return this.containerDefinitionOptions;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private GatlingFargateServiceProps serviceProps;
        private String serviceDiscoveryName;

        public Builder fargateServiceProps(GatlingFargateServiceProps props) {
            this.serviceProps = props;
            return this;
        }

        public Builder serviceDiscoveryName(String serviceDiscoveryName) {
            this.serviceDiscoveryName = serviceDiscoveryName;
            return this;
        }

        public GatlingDashboardFargateService build(Construct scope, String id) {
            return new GatlingDashboardFargateService(scope, id, this);
        }
    }
}
