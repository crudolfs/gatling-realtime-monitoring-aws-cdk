import * as cdk from '@aws-cdk/core';
import { Vpc, SubnetConfiguration, SubnetType, IVpc } from '@aws-cdk/aws-ec2';

export class GatlingVpcStack extends cdk.Stack {
  public vpc: Vpc;

  constructor(scope: cdk.Construct, id: string, namespace: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const privateSubnet: SubnetConfiguration = {
      name: 'gatling-private-subnet',
      cidrMask: 19,
      subnetType: SubnetType.PRIVATE
    };

    const publicSubnet: SubnetConfiguration = {
      name: 'gatling-public-subnet',
      cidrMask: 20,
      subnetType: SubnetType.PUBLIC
    };

    this.vpc = new Vpc(this, 'GatlingVpc', {
      cidr: '10.12.0.0/16',
      maxAzs: 2,
      subnetConfiguration: [privateSubnet, publicSubnet]
    });

    cdk.Tag.add(this.vpc, 'Name', `${namespace}-vpc`, {includeResourceTypes: ['AWS::EC2::VPC']} );
  }
}

export class ExistingVpcStack extends cdk.Stack {
  public vpc: IVpc;

	constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps, vpcName?: string) {
    super(scope, id, props);
  
    this.vpc = Vpc.fromLookup(this, 'gatlingVpc', {
      vpcName: vpcName
    });
  }
}
