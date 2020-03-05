package com.rudolfs.gatling.cdk.pipeline;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class GatlingPipelineStack extends Stack {

    public GatlingPipelineStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        PipelineProject cdkBuild = PipelineProject.Builder.create(this, "CDKBuild")
                .buildSpec(BuildSpec.fromSourceFilename("aws-cdk/buildspec.yml"))
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
                        .actionName("CDKBuild")
                        .input(sourceArtifact)
                        .outputs(singletonList(cdkBuildOutput))
                        .project(cdkBuild)
                        .build())
                )
                .build();

        StageProps deployStageProps = StageProps.builder()
                .stageName("Deploy")
                .actions(asList(CloudFormationCreateUpdateStackAction.Builder.create()
                                .actionName("SharedVpcCloudFormationDeploy")
                                .adminPermissions(true)
                                .stackName("SharedVpcStack")
                                .templatePath(cdkBuildOutput.atPath("SharedVpcStack.template.json"))
                                .build(),
                        CloudFormationCreateUpdateStackAction.Builder.create()
                                .actionName("GatlingEcrCloudFormationDeploy")
                                .adminPermissions(true)
                                .stackName("GatlingEcrStack")
                                .templatePath(cdkBuildOutput.atPath("GatlingEcrStack.template.json"))
                                .build(),
                        CloudFormationCreateUpdateStackAction.Builder.create()
                                .actionName("GatlingEcsFargateCloudFormationDeploy")
                                .adminPermissions(true)
                                .stackName("GatlingEcsFargateStack")
                                .templatePath(cdkBuildOutput.atPath("GatlingEcsFargateStack.template.json"))
                                .build()))
                .build();

        List<StageProps> stages = asList(sourceStageProps, buildStageProps, deployStageProps);

        Pipeline.Builder.create(this, "GatlingCICDPipeline")
                .stages(stages)
                .build();
    }
}
