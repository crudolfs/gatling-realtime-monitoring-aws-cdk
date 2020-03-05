package com.rudolfs.gatling.cdk;

import com.rudolfs.gatling.cdk.pipeline.GatlingPipelineStack;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Objects;

/**
 * AWS CDK app that creates the stack for the build and deployment pipeline of the Gatling Realtime Monitoring app.
 */
public class GatlingPipelineCdkApp {

    public static void main(final String[] args) {
        App app = new App();

        String account = Objects.requireNonNull(System.getenv("CDK_DEFAULT_ACCOUNT"), "CDK_DEFAULT_ACCOUNT is required.");
        String region = Objects.requireNonNull(System.getenv("CDK_DEFAULT_REGION"), "CDK_DEFAULT_REGION is required.");

        StackProps stackProps = StackProps.builder()
                .env(Environment.builder()
                        .account(account)
                        .region(region)
                        .build())
                .build();

        new GatlingPipelineStack(app, "GatlingPipelineStack", stackProps);

        app.synth();
    }
}
