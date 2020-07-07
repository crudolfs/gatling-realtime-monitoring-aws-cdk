#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "@aws-cdk/core";
import { GatlingVpcStack, ExistingVpcStack } from "../lib/gatling-vpc-stack";
import { GatlingEcsStack } from "../lib/gatling-ecs-stack";
import { StackProps } from "@aws-cdk/core";
import { IVpc } from "@aws-cdk/aws-ec2";

const app = new cdk.App();

const account = process.env.CDK_DEFAULT_ACCOUNT;
const region = process.env.CDK_DEFAULT_REGION;
const projectName = process.env.PROJECT_NAME ?? "gatling-ts";
const vpcName = process.env.VPC_NAME;
const vpcStackName = `${projectName}VpcStack`;
const ecsStackName = `${projectName}EcsStack`;

const stackProps: StackProps = {
  env: {
    account: account,
    region: region,
  },
};

let vpc: IVpc = vpcName == null
                ? new GatlingVpcStack(app, vpcStackName, projectName, stackProps).vpc
                : new ExistingVpcStack(app, vpcStackName, stackProps, vpcName).vpc;

new GatlingEcsStack(app, ecsStackName, { namespace: projectName, vpc: vpc }, stackProps);
