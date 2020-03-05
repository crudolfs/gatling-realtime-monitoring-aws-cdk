# AWS CDK project for Gatling realtime monitoring on AWS ECS
This module contains the [AWS CDK](https://docs.aws.amazon.com/cdk/latest/guide/home.html) code for 3 stacks:
1. SharedVpcStack: contains a VPC with a private and public subnet (redundant in two availability zones)
2. GatlingEcrStack: contains three ECR repositories (gatling-runner, grafana and influxdb)
3. GatlingEcsFargateStack: contains an ECS cluster with two services (one for the gatling-runner and one for the monitoring part with grafana and influxdb)

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands
 * `mvn package` compile and run tests
 * `cdk ls --profile <profile-name>` list all stacks in the app
 * `cdk synth SharedVpcStack --profile <profile-name>` emits the synthesized CloudFormation template for the SharedVpcStack
 * `cdk synth GatlingEcrStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingEcrStak
 * `cdk synth GatlingEcsFargateStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingEcsFargateStack
 * `cdk deploy SharedVpcStack --profile <profile-name>` deploy the SharedVpcStack to the AWS account/region as specified by the provided profile
 * `cdk deploy GatlingEcrStack --profile <profile-name>` deploy the GatlingEcrStack to the AWS account/region as specified by the provided profile
 * `cdk deploy GatlingEcsFargateStack --profile <profile-name>` deploy the GatlingEcsFargateStack to the AWS account/region as specified by the provided profile
  * `cdk diff` compare deployed stack with current state
  * `cdk docs` open CDK documentation
 
 ## Deploy instructions
 The stack should be deployed in the following order:
 1. SharedVpcStack
 2. GatlingEcrStack
 3. GatlingEcsFargateStack
 
 Use the --profile option to make sure that the stack is deployed to the correct AWS account/region.
 
 ```
cdk deploy SharedVpcStack --profile <profile-name>
cdk deploy GatlingEcrStack --profile <profile-name>
cdk deploy GatlingEcsFargateStack --profile <profile-name>
```
The deployment of these stacks is automated in a CodePipeline stack (see [aws-cdk-codepipeline](../aws-cdk-pipeline)).
