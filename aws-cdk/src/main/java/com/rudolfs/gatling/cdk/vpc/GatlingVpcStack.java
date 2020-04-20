package com.rudolfs.gatling.cdk.vpc;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.Tag;
import software.amazon.awscdk.core.TagProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

/**
 * Creates the CloudFormation for an AWS VPC with a private subnet and public subnet (both redundant in two availability zones).
 */
public class GatlingVpcStack extends Stack implements Supplier<IVpc> {
    private final Vpc vpc;

    public GatlingVpcStack(Construct scope, String id, StackProps stackProps, String namespace) {
        super(scope, id, stackProps);

        SubnetConfiguration privateSubnet = subnet("gatling-private-subnet", 19, SubnetType.PRIVATE);
        SubnetConfiguration publicSubnet = subnet("gatling-public-subnet", 20, SubnetType.PUBLIC);

        this.vpc = Vpc.Builder.create(this, "GatlingVpc")
                .cidr("10.12.0.0/16")
                .maxAzs(2)
                .subnetConfiguration(List.of(privateSubnet, publicSubnet))
                .build();

        Tag.add(vpc, "Name", namespace + "-vpc", TagProps.builder()
                .includeResourceTypes(singletonList("AWS::EC2::VPC"))
                .build());
    }

    SubnetConfiguration subnet(String name, Number cidrMask, SubnetType subnetType) {
        return SubnetConfiguration.builder()
                .cidrMask(cidrMask)
                .name(name)
                .subnetType(subnetType)
                .build();
    }

    @Override
    public IVpc get() {
        return this.vpc;
    }
}
