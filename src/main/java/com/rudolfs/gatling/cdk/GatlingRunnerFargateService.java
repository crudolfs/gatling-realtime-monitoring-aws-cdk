package com.rudolfs.gatling.cdk;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryAttributes;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.Arrays;

public class GatlingRunnerFargateService extends Construct {
    private final FargateService fargateService;

    public GatlingRunnerFargateService(Construct scope, String id, GatlingFargateServiceProps props) {
        super(scope, id);

        SecurityGroup securityGroup = new SecurityGroup(this, "GatlingRunnerSecurityGroup", SecurityGroupProps.builder()
                .vpc(props.getVpc())
                .description("gatling runner security group")
                .build());

        FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GatlingRunnerFargateTaskDefinition")
                .cpu(1024)
                .memoryLimitMiB(2048)
                .executionRole(props.getFargateExecutionRole())
                .taskRole(props.getFargateTaskRole())
                .build();

        ContainerDefinitionOptions containerDefinitionOptions = new GatlingRunnerContainerOptions(
                this,
                "GatlingRunnerContainerOptions",
                props.getStackProps().getEnv().getRegion(),
                props.getStackProps().getEnv().getAccount())
                .getContainerDefinitionOptions();
        fargateTaskDefinition.addContainer("gatlingRunnerContainer", containerDefinitionOptions);

        this.fargateService = FargateService.Builder.create(this, id)
                .serviceName("gatling-runner")
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(0)
                .cluster(props.getEcsCluster())
                .securityGroup(securityGroup)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(props.getVpc().getPrivateSubnets())
                        .build())
                .build();
    }

    public FargateService getFargateService() {
        return this.fargateService;
    }

    static class GatlingRunnerContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public GatlingRunnerContainerOptions(Construct scope, String id, String region, String accountId) {
            super(scope, id);

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromEcrRepository(Repository.fromRepositoryAttributes(
                            this,
                            "GatlingRunnerRepository",
                            RepositoryAttributes.builder()
                                    .repositoryArn(String.format("arn:aws:ecr:%s:%s:repository/gatling/gatling-runner", region, accountId))
                                    .repositoryName("gatling/gatling-runner")
                                    .build())))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "gatlingRunnerFargateLogGroup")
                                    .logGroupName("/ecs/gatling/gatling-runner")
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .build())
                            .streamPrefix("gatling-runner")
                            .build()))
                    .build();
        }

        public ContainerDefinitionOptions getContainerDefinitionOptions() {
            return this.containerDefinitionOptions;
        }
    }
}
