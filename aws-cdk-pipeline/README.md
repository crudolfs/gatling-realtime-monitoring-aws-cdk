# AWS CDK project for the pipeline of Gatling realtime monitoring on AWS ECS
This module contains the [AWS CDK](https://docs.aws.amazon.com/cdk/latest/guide/home.html) code for the CodePipeline stack:
- GatlingPipelineStack: contains the CodePipeline for builds and deployment of Gatling Realtime Monitoring on AWS ECS Fargate.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands
 * `mvn package` compile and run tests
 * `cdk ls --profile <profile-name>` list all stacks in the app
 * `cdk synth GatlingPipelineStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingPipelineStack
 * `cdk deploy GatlingPipelineStack --profile <profile-name>` deploy the GatlingPipelineStack to the AWS account/region as specified by the provided profile
  * `cdk diff` compare deployed stack with current state
  * `cdk docs` open CDK documentation
 
 ## Deploy instructions
Use the --profile option to make sure that the stack is deployed to the correct AWS account/region.
 
 ```
cdk deploy GatlingPipelineStack --profile <profile-name>
```
