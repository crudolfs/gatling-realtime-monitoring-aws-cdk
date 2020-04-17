package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.List;
import java.util.Map;

public class Ec2InstanceProfileRole extends Role {

    public Ec2InstanceProfileRole(Construct scope, String id, String roleNamePrefix) {
        this(scope, id, RoleProps.builder()
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .inlinePolicies(Map.of("rexrayVolumeDriverPolicy", RexRayPolicyDocument.builder().build()))
                .roleName(roleNamePrefix + "-ecs-ec2-role")
                .build());
    }

    public Ec2InstanceProfileRole(Construct scope, String id, RoleProps props) {
        super(scope, id, props);
    }

    private static final class RexRayPolicyDocument {

        private static List<PolicyStatement> policyStatements() {
            return List.of(PolicyStatement.Builder.create()
                    .effect(Effect.ALLOW)
                    .actions(List.of(
                            "ec2:AttachVolume",
                            "ec2:CreateVolume",
                            "ec2:CreateSnapshot",
                            "ec2:CreateTags",
                            "ec2:DeleteVolume",
                            "ec2:DeleteSnapshot",
                            "ec2:DescribeAvailabilityZones",
                            "ec2:DescribeInstances",
                            "ec2:DescribeVolumes",
                            "ec2:DescribeVolumeAttribute",
                            "ec2:DescribeVolumeStatus",
                            "ec2:DescribeSnapshots",
                            "ec2:CopySnapshot",
                            "ec2:DescribeSnapshotAttribute",
                            "ec2:DetachVolume",
                            "ec2:ModifySnapshotAttribute",
                            "ec2:ModifyVolumeAttribute",
                            "ec2:DescribeTags"
                    ))
                    .resources(List.of("*"))
                    .build()
            );
        }

        public static RexRayPolicyDocument.Builder builder() {
            return new RexRayPolicyDocument.Builder();
        }

        public static final class Builder {
            public PolicyDocument build() {
                return PolicyDocument.Builder.create()
                        .statements(policyStatements())
                        .build();
            }
        }
    }
}
