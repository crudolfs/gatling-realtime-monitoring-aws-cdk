# AWS CDK project for Gatling realtime monitoring on AWS ECS
This module contains the [AWS CDK](https://docs.aws.amazon.com/cdk/latest/guide/home.html) code for 3 stacks:
1. GatlingVpcStack: contains a VPC with a private and public subnet (redundant in two availability zones)
2. GatlingEcrStack: contains three ECR repositories (gatling-runner, grafana and influxdb)
3. GatlingEcsFargateStack: contains an ECS cluster with two services (one for the gatling-runner and one for the monitoring part with grafana and influxdb)

## AWS CDK installation
The AWS CDK command line tool (cdk) and the AWS Construct Library are developed in TypeScript and run on Node.js.
Therefore you must have Node.js version >= 10.3.0 installed. Then install the AWS CDK by running the following command:

`npm install -g aws-cdk`

Then verify your installation:

`cdk --version`

### Prerequisites
This is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.
Make sure the following is in place for Java:

- Maven 3.5.4 or higher
- Java 8 (<<TODO: check if AWS CDK runs with Java 11+>>)
- Specify AWS credentials and region

and in general:

- [Specify AWS credentials and region](https://docs.aws.amazon.com/cdk/latest/guide/getting_started.html#getting_started_credentials)

The configuration of AWS credentials and region are very important, it's key to make the infrastructure deployment work correctly.

**Note**: the aws-mfa tool may help when MFA is enforced on your AWS account: https://github.com/broamski/aws-mfa

## Useful commands
 * `mvn package` compile and run tests
 * `cdk ls --profile <profile-name>` list all stacks in the app
 * `cdk synth GatlingVpcStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingVpcStack
 * `cdk synth GatlingEcrStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingEcrStack
 * `cdk synth GatlingEcsFargateStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingEcsFargateStack
 * `cdk synth GatlingPipelineStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingPipelineStack
 * `cdk deploy GatlingVpcStack --profile <profile-name>` deploy the GatlingVpcStack to the AWS account/region as specified by the provided profile
 * `cdk deploy GatlingEcrStack --profile <profile-name>` deploy the GatlingEcrStack to the AWS account/region as specified by the provided profile
 * `cdk deploy GatlingEcsFargateStack --profile <profile-name>` deploy the GatlingEcsFargateStack to the AWS account/region as specified by the provided profile
 * `cdk deploy GatlingPipelineStack --profile <profile-name>` deploy the GatlingPipelineStack to the AWS account/region as specified by the provided profile
 * `cdk diff` compare deployed stack with current state
 * `cdk docs` open CDK documentation
 
 ## Deploy instructions
 The stack should be deployed in the following order:
 1. GatlingVpcStack (optional)
 2. GatlingEcrStack
 3. GatlingEcsFargateStack
 4. GatlingPipelineStack (optional)
 
 Use the --profile option to make sure that the stack is deployed to the correct AWS account/region.
 
 ```
cdk deploy GatlingVpcStack --profile <profile-name>
cdk deploy GatlingEcrStack --profile <profile-name>
cdk deploy GatlingEcsFargateStack --profile <profile-name>
```
The deployment of these stacks is automated in a CodePipeline stack 
(see [GatlingPipelineStack](../aws-cdk/src/main/java/com/rudolfs/gatling/cdk/pipeline/GatlingPipelineStack.java)).
The CodePipeline itself is also written in infrastructure code and can be deployed with the following command:

`cdk deploy GatlingPipelineStack --profile <profile-name>`.
