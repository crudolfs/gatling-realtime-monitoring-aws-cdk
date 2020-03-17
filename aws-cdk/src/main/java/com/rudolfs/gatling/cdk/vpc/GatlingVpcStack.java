package com.rudolfs.gatling.cdk.vpc;

import com.rudolfs.gatling.cdk.StackBuilder;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.Tag;
import software.amazon.awscdk.core.TagProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.Arrays;

import static java.util.Collections.singletonList;

/**
 * Creates the CloudFormation for an AWS VPC with a private subnet and public subnet (both redundant in two availability zones).
 */
public class GatlingVpcStack extends Stack {

    private GatlingVpcStack(Builder builder) {
        super(builder.getScope(), builder.getId(), builder.getStackProps());

        SubnetConfiguration privateSubnet = subnet("gatling-private-subnet", 19, SubnetType.PRIVATE);
        SubnetConfiguration publicSubnet = subnet("gatling-public-subnet", 20, SubnetType.PUBLIC);

        Vpc vpc = Vpc.Builder.create(this, "GatlingVpc")
                .cidr("10.12.0.0/16")
                .maxAzs(2)
                .subnetConfiguration(Arrays.asList(privateSubnet, publicSubnet))
                .build();

        Tag.add(vpc, "Name", builder.vpcName, TagProps.builder()
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StackBuilder<Builder> {
        private String vpcName;

        public Builder vpcName(String vpcName) {
            this.vpcName = vpcName;
            return this;
        }

        @Override
        public Stack build() {
            return new GatlingVpcStack(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
