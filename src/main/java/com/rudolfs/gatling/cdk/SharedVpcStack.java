package com.rudolfs.gatling.cdk;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.Arrays;

import static java.util.Collections.singletonList;

public class SharedVpcStack extends Stack {

    public SharedVpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        SubnetConfiguration privateSubnet = subnet("shared-private-subnet", 19, SubnetType.PRIVATE);
        SubnetConfiguration publicSubnet = subnet("shared-public-subnet", 20, SubnetType.PUBLIC);

        Vpc vpc = Vpc.Builder.create(this, "SharedVpc")
                .cidr("10.12.0.0/16")
                .subnetConfiguration(Arrays.asList(privateSubnet, publicSubnet))
                .build();

        Tag.add(vpc, "Name", "shared-vpc", TagProps.builder()
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
}
