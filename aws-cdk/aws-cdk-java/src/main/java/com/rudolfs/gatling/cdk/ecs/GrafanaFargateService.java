package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
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

public class GrafanaFargateService extends Construct {

    public GrafanaFargateService(Construct scope, String id, Builder builder) {
        super(scope, id);

        SecurityGroup securityGroup = new SecurityGroup(this, "GrafanaSecurityGroup", SecurityGroupProps.builder()
                .vpc(builder.serviceProps.getVpc())
                .description(String.format("%s security group", builder.serviceProps.getServiceName()))
                .build());
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3000), "The default port that runs the Grafana UI.");

        FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GrafanaTaskDefinition")
                .cpu(1024)
                .memoryLimitMiB(2048)
                .executionRole(builder.serviceProps.getFargateExecutionRole())
                .taskRole(builder.serviceProps.getFargateTaskRole())
                .build();

        ContainerDefinitionOptions grafanaContainerDefinitionOptions = new GrafanaContainerOptions(this, "GrafanaContainerOptions", builder)
                .getContainerDefinitionOptions();
        fargateTaskDefinition.addContainer(builder.serviceProps.getServiceName(), grafanaContainerDefinitionOptions);


        FargateService.Builder.create(this, id)
                .serviceName(builder.serviceProps.getServiceName())
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(0)
                .cloudMapOptions(CloudMapOptions.builder()
                        .cloudMapNamespace(builder.serviceProps.getEcsCluster().getDefaultCloudMapNamespace())
                        .dnsRecordType(DnsRecordType.A)
                        .name(builder.serviceProps.getServiceName())
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

        public GrafanaContainerOptions(Construct scope, String id, GrafanaFargateService.Builder builder) {
            super(scope, id);

            Map<String, String> environmentVariables = new HashMap<>();
            environmentVariables.put("INFLUXDB_ACCESS_MODE", "proxy");
            environmentVariables.put("INFLUXDB_HOST", builder.influxdbHostName);
            environmentVariables.put("INFLUXDB_PORT", "8086");

            DockerImageAsset grafanaAsset = DockerImageAsset.Builder.create(this, "grafanaAsset")
                    .directory("../../gatling-monitoring/grafana")
                    .build();

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromDockerImageAsset(grafanaAsset))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "grafanaFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s/%s", builder.serviceProps.getClusterNamespace(), builder.serviceProps.getServiceName()))
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .removalPolicy(RemovalPolicy.DESTROY)
                                    .build())
                            .streamPrefix(builder.serviceProps.getServiceName())
                            .build()))
                    .environment(environmentVariables)
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
        private GatlingEcsServiceProps serviceProps;
        private String influxdbHostName;

        public Builder fargateServiceProps(GatlingEcsServiceProps props) {
            this.serviceProps = props;
            return this;
        }

        public Builder influxdbHostName(String influxdbHostName) {
            this.influxdbHostName = influxdbHostName;
            return this;
        }

        public GrafanaFargateService build(Construct scope, String id) {
            return new GrafanaFargateService(scope, id, this);
        }
    }
}
