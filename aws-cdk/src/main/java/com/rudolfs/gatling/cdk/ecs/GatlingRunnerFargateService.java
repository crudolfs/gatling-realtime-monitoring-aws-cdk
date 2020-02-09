package com.rudolfs.gatling.cdk.ecs;

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

    private GatlingRunnerFargateService(Construct scope, String id, Builder builder) {
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

        final String namespace = builder.serviceProps.getClusterNamespace();
        final String serviceName = builder.serviceProps.getServiceName();
        final String repositoryName = String.format("%s/%s", namespace, serviceName);
        final String logGroupName = String.format("/ecs/%s/%s", builder.serviceProps.getClusterNamespace(), builder.serviceProps.getServiceName());

        ContainerDefinitionOptions containerDefinitionOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromEcrRepository(Repository.fromRepositoryAttributes(
                        this,
                        "GatlingRunnerRepository",
                        RepositoryAttributes.builder()
                                .repositoryArn(String.format("arn:aws:ecr:%s:%s:repository/%s/%s",
                                        builder.serviceProps.getStackProps().getEnv().getRegion(),
                                        builder.serviceProps.getStackProps().getEnv().getAccount(),
                                        namespace,
                                        serviceName))
                                .repositoryName(repositoryName)
                                .build())))
                .command(Arrays.asList(
                        "-gh",
                        builder.serviceProps.monitoringServiceDiscoveryEndpoint()
                ))
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "gatlingRunnerFargateLogGroup")
                                .logGroupName(logGroupName)
                                .retention(RetentionDays.TWO_WEEKS)
                                .build())
                        .streamPrefix(builder.serviceProps.getServiceName())
                        .build()))
                .build();

        fargateTaskDefinition.addContainer(builder.gatlingRunnerContainerName, containerDefinitionOptions);

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
        private GatlingFargateServiceProps serviceProps;
        private String gatlingRunnerContainerName;


        public Builder fargateServiceProps(GatlingFargateServiceProps props) {
            this.serviceProps = props;
            return this;
        }

        public Builder gatlingRunnerContainerName(String gatlingRunnerContainerName) {
            this.gatlingRunnerContainerName = gatlingRunnerContainerName;
            return this;
        }

        public GatlingRunnerFargateService build(Construct scope, String id) {
            return new GatlingRunnerFargateService(scope, id, this);
        }
    }
}
