# AWS CDK project for Gatling realtime monitoring on AWS ECS
This module contains an [AWS CDK](https://docs.aws.amazon.com/cdk/latest/guide/home.html) app for a Gatling Realtime Monitoring solution on AWS ECS that 
consists of the following 2 stacks:
1. GatlingVpcStack: contains a VPC with a private and public subnet (redundant in two availability zones)
2. GatlingEcsStack: contains an ECS cluster with three services (gatling-runner, grafana and influxdb)

## AWS CDK installation
The AWS CDK command line tool (cdk) and the AWS Construct Library are developed in TypeScript and run on Node.js.
Therefore you must have Node.js version >= 10.3.0 installed. Installing the AWS CDK is easy:

`npm install -g aws-cdk`

Then verify your installation:

`cdk --version`

### Prerequisites
This is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.
Make sure the following is in place for Java:

- Maven 3.6.3
- Java 11

and in general:

- [Specify AWS credentials and region](https://docs.aws.amazon.com/cdk/latest/guide/getting_started.html#getting_started_credentials)

The configuration of AWS credentials and region are very important, it's key to make the infrastructure deployment work correctly.
Make sure that when using the --profile option you specify the credentials in the ~/.aws/config file instead of the ~/.aws/credentials file 
which is normally the case when using profiles with the AWS CLI.  

**Note**: The aws-mfa tool may be helpful when MFA is enforced on your AWS account: https://github.com/broamski/aws-mfa

## Useful commands
 * `mvn package` compile and run tests
 * `cdk ls --profile <profile-name>` list all stacks in the app
 * `cdk synth GatlingVpcStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingVpcStack
 * `cdk synth GatlingEcsStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingEcsFargateStack
 * `cdk synth GatlingPipelineStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingPipelineStack
 * `cdk deploy GatlingVpcStack --profile <profile-name>` deploy the GatlingVpcStack to the AWS account/region as specified by the provided profile
 * `cdk deploy GatlingEcsStack --profile <profile-name>` deploy the GatlingEcsFargateStack to the AWS account/region as specified by the provided profile
 * `cdk deploy GatlingPipelineStack --profile <profile-name>` deploy the GatlingPipelineStack to the AWS account/region as specified by the provided profile
 * `cdk diff` compare deployed stack with current state
 * `cdk docs` open CDK documentation
 
 ## Deploy instructions
 The stack should be deployed in the following order:
 1. GatlingVpcStack (optional)
 2. GatlingEcsStack
 3. GatlingPipelineStack (optional)
 
 Use the --profile option to make sure that the stack is deployed to the correct AWS account/region.
 
 ```
cdk deploy GatlingVpcStack --profile <profile-name>
cdk deploy GatlingEcsStack --profile <profile-name>
```
The deployment of these stacks is automated in a CodePipeline stack 
(see [GatlingPipelineStack](../aws-cdk/src/main/java/com/rudolfs/gatling/cdk/pipeline/GatlingPipelineStack.java)).
The CodePipeline itself is also written in infrastructure code and can be deployed with the following command:

`cdk deploy GatlingPipelineStack --profile <profile-name>`.
