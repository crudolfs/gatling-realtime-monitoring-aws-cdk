package com.rudolfs.gatling.cdk;

import com.rudolfs.gatling.cdk.ecs.GatlingEcsStack;
import com.rudolfs.gatling.cdk.pipeline.GatlingPipelineStack;
import com.rudolfs.gatling.cdk.vpc.ExistingVpcStack;
import com.rudolfs.gatling.cdk.vpc.GatlingVpcStack;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * AWS CDK app that contains the stacks for a Gatling Realtime Monitoring app.
 */
public class GatlingRealtimeMonitoringCdkApp {
    private static final String DEFAULT_PROJECT_NAME = "gatling";

    public static void main(final String[] args) {
        App app = new App();

        final String account = Objects.requireNonNull(System.getenv("CDK_DEFAULT_ACCOUNT"), "CDK_DEFAULT_ACCOUNT is required.");
        final String region = Objects.requireNonNull(System.getenv("CDK_DEFAULT_REGION"), "CDK_DEFAULT_REGION is required.");
        final String projectName = System.getenv("PROJECT_NAME") == null ? DEFAULT_PROJECT_NAME : System.getenv("PROJECT_NAME");
        final String vpcName = System.getenv("VPC_NAME");
        final String vpcStackName = projectName + "VpcStack";
        final String ecsStackName = projectName + "EcsStack";
        final String pipelineStackName = projectName + "PipelineStack";
        final String pipelineName = projectName + "-pipeline";

        StackProps stackProps = StackProps.builder()
                .env(Environment.builder()
                        .account(account)
                        .region(region)
                        .build())
                .build();

        Supplier<IVpc> vpcSupplier = vpcName == null ?
                new GatlingVpcStack(app, vpcStackName, stackProps, projectName) :
                new ExistingVpcStack(app, vpcStackName, stackProps, vpcName);

        GatlingEcsStack.builder().scope(app).id(ecsStackName).stackProps(stackProps)
                .namespace(projectName)
                .ecsClusterName(projectName + "-cluster")
                .vpc(vpcSupplier)
                .build();

        GatlingPipelineStack.builder().scope(app).id(pipelineStackName).stackProps(stackProps)
                .pipelineName(pipelineName)
                .vpcStackName(vpcStackName)
                .ecsStackName(ecsStackName)
                .build();

        app.synth();
    }
}
