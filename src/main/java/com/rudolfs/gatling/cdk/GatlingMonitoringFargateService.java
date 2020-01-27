package com.rudolfs.gatling.cdk;

import software.amazon.awscdk.core.Arn;
import software.amazon.awscdk.core.ArnComponents;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryAttributes;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

public class GatlingMonitoringFargateService extends Construct {
    private final FargateService fargateService;

    public GatlingMonitoringFargateService(Construct scope, String id, GatlingFargateServiceProps props) {
        super(scope, id);

        SecurityGroup securityGroup = new SecurityGroup(this, "GrafanaInfluxSecurityGroup", SecurityGroupProps.builder()
                .vpc(props.getVpc())
                .description("grafana and influxdb security group")
                .build());
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8083), "The default port that runs the InfluxDB Admin UI.");
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8086), "The default port that runs the InfluxDB HTTP service.");
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(2003), "The default port that runs the Graphite service.");

        FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GrafanaInfluxTaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .executionRole(props.getFargateExecutionRole())
                .taskRole(props.getFargateTaskRole())
                .build();

        ContainerDefinitionOptions grafanaContainerDefinitionOptions = new GrafanaContainerOptions(
                this,
                "GrafanaContainerOptions",
                props.getStackProps().getEnv().getRegion(),
                props.getStackProps().getEnv().getAccount())
                .getContainerDefinitionOptions();
        fargateTaskDefinition.addContainer("grafanaContainer", grafanaContainerDefinitionOptions);

        ContainerDefinitionOptions influxContainerDefinitionOptions = new InfluxContainerOptions(
                this,
                "InfluxDBContainerOptions",
                props.getStackProps().getEnv().getRegion(),
                props.getStackProps().getEnv().getAccount())
                .getContainerDefinitionOptions();
        fargateTaskDefinition.addContainer("influxdbContainer", influxContainerDefinitionOptions);

        this.fargateService = FargateService.Builder.create(this, id)
                .serviceName("grafana-influxdb")
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(0)
                .cluster(props.getEcsCluster())
                .securityGroup(securityGroup)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(props.getVpc().getPublicSubnets())
                        .build())
                .build();
    }

    public FargateService getFargateService() {
        return this.fargateService;
    }

    static class GrafanaContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public GrafanaContainerOptions(Construct scope, String id, String region, String accountId) {
            super(scope, id);

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromEcrRepository(
                            Repository.fromRepositoryAttributes(this, "GatlingGrafanaRepository", repositoryAttributes("gatling/grafana", region, accountId))))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "grafanaFargateLogGroup")
                                    .logGroupName("/ecs/grafana")
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .build())
                            .streamPrefix("grafana")
                            .build()))
                    .build();
        }

        public ContainerDefinitionOptions getContainerDefinitionOptions() {
            return this.containerDefinitionOptions;
        }
    }

    static class InfluxContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public InfluxContainerOptions(Construct scope, String id, String region, String accountId) {
            super(scope, id);

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromEcrRepository(
                            Repository.fromRepositoryAttributes(this, "GatlingInfluxDBRepository", repositoryAttributes("gatling/influxdb", region, accountId))))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "influxdbFargateLogGroup")
                                    .logGroupName("/ecs/influxdb")
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .build())
                            .streamPrefix("influxdb")
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
}
