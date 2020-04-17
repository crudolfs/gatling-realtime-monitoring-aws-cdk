package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.List;

public class GatlingRunnerFargateService extends Construct {

    public GatlingRunnerFargateService(Construct scope, String id, Builder builder) {
        super(scope, id);

        SecurityGroup securityGroup = new SecurityGroup(this, "GatlingRunnerSecurityGroup", SecurityGroupProps.builder()
                .vpc(builder.serviceProps.getVpc())
                .description(String.format("%s security group", builder.serviceProps.getServiceName()))
                .build());

        FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GatlingRunnerFargateTaskDefinition")
                .cpu(1024)
                .memoryLimitMiB(2048)
                .executionRole(builder.serviceProps.getFargateExecutionRole())
                .taskRole(builder.serviceProps.getFargateTaskRole())
                .build();

        String logGroupName = String.format("/ecs/%s/%s", builder.serviceProps.getClusterNamespace(), builder.serviceProps.getServiceName());

        DockerImageAsset gatlingRunnerAsset = DockerImageAsset.Builder.create(this, "gatlingRunnerAsset")
                .directory("../gatling-monitoring/gatling-runner")
                .build();

        ContainerDefinitionOptions containerDefinitionOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromDockerImageAsset(gatlingRunnerAsset))
                .command(List.of("-gh", builder.influxdbHostName))
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "gatlingRunnerFargateLogGroup")
                                .logGroupName(logGroupName)
                                .retention(RetentionDays.TWO_WEEKS)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .build())
                        .streamPrefix(builder.serviceProps.getServiceName())
                        .build()))
                .build();

        fargateTaskDefinition.addContainer("gatlingRunnerContainer", containerDefinitionOptions);

        FargateService.Builder.create(this, id)
                .serviceName(builder.serviceProps.getServiceName())
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(0)
                .cluster(builder.serviceProps.getEcsCluster())
                .securityGroup(securityGroup)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(builder.serviceProps.getVpc().getPrivateSubnets())
                        .build())
                .build();
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

        public GatlingRunnerFargateService build(Construct scope, String id) {
            return new GatlingRunnerFargateService(scope, id, this);
        }
    }
}
