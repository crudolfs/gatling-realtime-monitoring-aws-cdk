import * as cdk from "@aws-cdk/core";
import {
  Role,
  ServicePrincipal,
  PolicyDocument,
  PolicyStatement,
  Effect,
  ManagedPolicy,
} from "@aws-cdk/aws-iam";

export class Ec2InstanceProfileRole extends Role {
  constructor(scope: cdk.Construct, id: string, roleNamePrefix: string) {
    super(scope, id, {
      assumedBy: new ServicePrincipal("ec2.amazonaws.com"),
      inlinePolicies: { "rexrayVolumeDriverPolicy": createRexrayVolumeDriverPolicyDocument() },
      roleName: `${roleNamePrefix}-ecs-ec2-role`,
    });
  }
}

export class FargateExecutionRole extends Role {
  constructor(scope: cdk.Construct, id: string, roleNamePrefix: string) {
    super(scope, id, {
      assumedBy: new ServicePrincipal("ecs-tasks.amazonaws.com"),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName(
          "service-role/AmazonECSTaskExecutionRolePolicy"
        ),
      ],
      roleName: `${roleNamePrefix}-ecs-execution-role`,
    });
  }
}

export class FargateTaskRole extends Role {
  constructor(scope: cdk.Construct, id: string, roleNamePrefix: string) {
    super(scope, id, {
      assumedBy: new ServicePrincipal("ecs-tasks.amazonaws.com"),
      inlinePolicies: { "cloudwatch-logs": new PolicyDocument({
        statements: [
          new PolicyStatement( {
            effect: Effect.ALLOW,
            actions: ["logs:*"],
            resources: ["*"]
          })
        ]
      }) },
      roleName: `${roleNamePrefix}-ecs-task-role`,
    });
  }
}

function createRexrayVolumeDriverPolicyDocument(): PolicyDocument {
  return new PolicyDocument({
    statements: [
      new PolicyStatement({
        effect: Effect.ALLOW,
        actions: [
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
          "ec2:DescribeTags",
        ],
        resources: ["*"],
      }),
    ],
  });
}
