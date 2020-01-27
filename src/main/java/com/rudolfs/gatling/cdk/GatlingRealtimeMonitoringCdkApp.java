package com.rudolfs.gatling.cdk;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Objects;

public class GatlingRealtimeMonitoringCdkApp {
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

        new SharedVpcStack(app, "SharedVpcStack", stackProps);
        new GatlingEcrStack(app, "GatlingEcrStack", stackProps);
        new GatlingEcsFargateStack(app, "GatlingEcsFargateStack", stackProps);

        app.synth();
    }
}
