package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.iam.*;

import static java.util.Collections.singletonList;

public class FargateExecutionRole extends Role {

    public FargateExecutionRole(Construct scope, String id) {
        this(scope, id, RoleProps.builder()
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(singletonList(ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonECSTaskExecutionRolePolicy")))
                .roleName("gatling-ecs-execution-role")
                .build());
    }

    public FargateExecutionRole(Construct scope, String id, RoleProps props) {
        super(scope, id, props);
    }
}
