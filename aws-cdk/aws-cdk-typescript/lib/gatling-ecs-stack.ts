import * as cdk from '@aws-cdk/core';
import { Vpc, IVpc } from '@aws-cdk/aws-ec2';
import { Cluster } from '@aws-cdk/aws-ecs';
import { NamespaceType } from '@aws-cdk/aws-servicediscovery';

export interface GatlingEcsStackProps {
  readonly vpc: IVpc;
  readonly namespace: string;
}

export class GatlingEcsStack extends cdk.Stack {

  constructor(scope: cdk.Construct, id: string, gatlingEcsStackProps: GatlingEcsStackProps, props?: cdk.StackProps) {
    super(scope, id, props);

    const ecsClusterName: string = `${gatlingEcsStackProps?.namespace}-cluster`;

    const cluster = new Cluster(this, 'GatlingCluster', {
      clusterName: ecsClusterName,
      vpc: gatlingEcsStackProps.vpc,
      defaultCloudMapNamespace: {
        name: gatlingEcsStackProps.namespace,
        type: NamespaceType.DNS_PRIVATE
      }
    });
  }
}
