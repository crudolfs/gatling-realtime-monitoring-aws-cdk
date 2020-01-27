package com.rudolfs.gatling.cdk;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.iam.*;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

public class FargateTaskRole extends Role {

    public FargateTaskRole(Construct scope, String id) {
        this(scope, id, RoleProps.builder()
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .inlinePolicies(createInlinePolicies())
                .roleName("gatling-ecs-task-role")
                .build());
    }

    public FargateTaskRole(Construct scope, String id, RoleProps props) {
        super(scope, id, props);
    }

    private static Map<String, PolicyDocument> createInlinePolicies() {
        Map<String, PolicyDocument> inlinePolicies = new HashMap<>();
        inlinePolicies.put("cloudwatch-logs", PolicyDocument.Builder.create()
                .statements(singletonList(PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(singletonList("logs:*"))
                        .resources(singletonList("*"))
                        .build()))
                .build());
        return inlinePolicies;
    }
}
