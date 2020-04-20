package com.rudolfs.gatling.cdk.vpc;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;

import java.util.function.Supplier;

/**
 * Creates the CloudFormation for an AWS VPC with a private subnet and public subnet (both redundant in two availability zones).
 */
public class ExistingVpcStack extends Stack implements Supplier<IVpc> {
    private final IVpc vpc;

    public ExistingVpcStack(Construct scope, String id, StackProps stackProps, String vpcName) {
        super(scope, id, stackProps);

        this.vpc = Vpc.fromLookup(this, "gatlingVpc", VpcLookupOptions.builder()
                .vpcName(vpcName)
                .build());
    }

    @Override
    public IVpc get() {
        return this.vpc;
    }
}
