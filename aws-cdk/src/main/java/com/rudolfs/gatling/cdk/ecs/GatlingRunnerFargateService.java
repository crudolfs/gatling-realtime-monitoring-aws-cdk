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

    public GatlingRunnerFargateService(Construct scope, String id, GatlingFargateServiceProps serviceProps) {
        super(scope, id);

        SecurityGroup securityGroup = new SecurityGroup(this, "GatlingRunnerSecurityGroup", SecurityGroupProps.builder()
                .vpc(serviceProps.getVpc())
                .description(String.format("%s security group", serviceProps.getServiceName()))
                .build());

        FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GatlingRunnerFargateTaskDefinition")
                .cpu(1024)
                .memoryLimitMiB(2048)
                .executionRole(serviceProps.getFargateExecutionRole())
                .taskRole(serviceProps.getFargateTaskRole())
                .build();

        final String logGroupName = String.format("/ecs/%s/%s", serviceProps.getClusterNamespace(), serviceProps.getServiceName());

        DockerImageAsset gatlingRunnerAsset = DockerImageAsset.Builder.create(this, "gatlingRunnerAsset")
                .directory("../gatling-monitoring/gatling-runner")
                .build();

        ContainerDefinitionOptions containerDefinitionOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromDockerImageAsset(gatlingRunnerAsset))
                .command(List.of("-gh", serviceProps.getGatlingDashboardServiceDiscoveryEndpoint()))
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "gatlingRunnerFargateLogGroup")
                                .logGroupName(logGroupName)
                                .retention(RetentionDays.TWO_WEEKS)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .build())
                        .streamPrefix(serviceProps.getServiceName())
                        .build()))
                .build();

        fargateTaskDefinition.addContainer("gatlingRunnerContainer", containerDefinitionOptions);

        FargateService.Builder.create(this, id)
                .serviceName(serviceProps.getServiceName())
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(0)
                .cluster(serviceProps.getEcsCluster())
                .securityGroup(securityGroup)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(serviceProps.getVpc().getPrivateSubnets())
                        .build())
                .build();
    }
}
