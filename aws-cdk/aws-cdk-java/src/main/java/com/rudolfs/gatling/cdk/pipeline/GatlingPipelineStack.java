package com.rudolfs.gatling.cdk.pipeline;

import com.rudolfs.gatling.cdk.StackBuilder;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CloudFormationCreateUpdateStackAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;

import java.util.List;

import static java.util.Collections.singletonList;

public class GatlingPipelineStack extends Stack {

    private GatlingPipelineStack(Builder builder) {
        super(builder.getScope(), builder.getId(), builder.getStackProps());

        PipelineProject cdkBuild = PipelineProject.Builder.create(this, "CDKBuild")
                .buildSpec(BuildSpec.fromSourceFilename("aws-cdk/aws-cdk-java/buildspec.yml"))
                .environment(BuildEnvironment.builder()
                        .buildImage(LinuxBuildImage.STANDARD_2_0).build())
                .build();

        Artifact sourceArtifact = new Artifact();
        Artifact cdkBuildOutput = new Artifact("CdkBuildOutput");

        StageProps sourceStageProps = StageProps.builder()
                .stageName("Source")
                .actions(singletonList(GitHubSourceAction.Builder.create()
                        .actionName("CheckoutSource")
                        .branch("master")
                        .oauthToken(SecretValue.secretsManager("gatling-realtime-monitoring-secret"))
                        .output(sourceArtifact)
                        .owner("crudolfs")
                        .repo("gatling-realtime-monitoring-aws-cdk")
                        .build()))
                .build();

        StageProps buildStageProps = StageProps.builder()
                .stageName("Build")
                .actions(singletonList(CodeBuildAction.Builder.create()
                        .actionName("AwsCdkBuild")
                        .input(sourceArtifact)
                        .outputs(singletonList(cdkBuildOutput))
                        .project(cdkBuild)
                        .build())
                )
                .build();

        StageProps deployStageProps = StageProps.builder()
                .stageName("Deploy")
                .actions(List.of(CloudFormationCreateUpdateStackAction.Builder.create()
                                .actionName("VpcStackUpdate")
                                .adminPermissions(true)
                                .runOrder(1)
                                .stackName(builder.vpcStackName)
                                .templatePath(cdkBuildOutput.atPath(builder.vpcStackName + ".template.json"))
                                .build(),
                        CloudFormationCreateUpdateStackAction.Builder.create()
                                .actionName("EcsStackUpdate")
                                .adminPermissions(true)
                                .runOrder(3)
                                .stackName(builder.ecsStackName)
                                .templatePath(cdkBuildOutput.atPath(builder.ecsStackName + ".template.json"))
                                .build()))
                .build();

        List<StageProps> stages = List.of(sourceStageProps, buildStageProps, deployStageProps);

        Pipeline.Builder.create(this, "GatlingCICDPipeline")
                .pipelineName(builder.pipelineName)
                .stages(stages)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StackBuilder<Builder> {
        private String pipelineName;
        private String vpcStackName;
        private String ecsStackName;

        public Builder pipelineName(String pipelineName) {
            this.pipelineName = pipelineName;
            return this;
        }

        public Builder vpcStackName(String vpcStackName) {
            this.vpcStackName = vpcStackName;
            return this;
        }

        public Builder ecsStackName(String ecsStackName) {
            this.ecsStackName = ecsStackName;
            return this;
        }

        @Override
        public GatlingPipelineStack build() {
            return new GatlingPipelineStack(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
