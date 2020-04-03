package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.TagStatus;
import software.amazon.awscdk.services.iam.ArnPrincipal;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Creates the CloudFormation for the AWS ECR repositories for the gatling runner, grafana and influxdb docker images.
 */
public class GatlingEcrStack extends Stack {
    private final String accountId;

    public GatlingEcrStack(Construct scope, String id, StackProps stackProps, GatlingEcrProps ecrProps) {
        super(scope, id, stackProps);

        this.accountId = stackProps.getEnv().getAccount();

        final int maxImageCount = 5;

        LifecycleRule lifecycleRule = LifecycleRule.builder()
                .rulePriority(1)
                .description(String.format("Only keep the last %d images", maxImageCount))
                .maxImageCount(maxImageCount)
                .tagStatus(TagStatus.ANY)
                .build();

        List<LifecycleRule> lifecycleRules = Collections.singletonList(lifecycleRule);

        buildEcrRepository("EcrRepositoryGatlingRunner", ecrProps.getRepositoryNameWithNamespace(ecrProps.getGatlingRunnerRepositoryName()), lifecycleRules);
        buildEcrRepository("EcrRepositoryGrafana", ecrProps.getRepositoryNameWithNamespace(ecrProps.getGrafanaRepositoryName()), lifecycleRules);
        buildEcrRepository("EcrRepositoryInfluxDB", ecrProps.getRepositoryNameWithNamespace(ecrProps.getInfluxDBRepositoryName()), lifecycleRules);
    }

    private void buildEcrRepository(String id, String repositoryName, List<LifecycleRule> lifecycleRules) {
        Repository ecrRepository = Repository.Builder.create(this, id)
                .repositoryName(repositoryName)
                .lifecycleRules(lifecycleRules)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        PolicyStatement policyStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .principals(Collections.singletonList(new ArnPrincipal("arn:aws:iam::" + this.accountId + ":root")))
                .actions(Arrays.asList(
                        "ecr:GetDownloadUrlForLayer",
                        "ecr:Batch*",
                        "ecr:PutImage",
                        "ecr:ListImages",
                        "ecr:InitiateLayerUpload",
                        "ecr:UploadLayerPart",
                        "ecr:CompleteLayerUpload",
                        "ecr:DescribeImages"))
                .build();
        policyStatement.setSid("AllowAccessFromAccount");

        ecrRepository.addToResourcePolicy(policyStatement);
    }
}
