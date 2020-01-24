package com.rudolfs.gatling.cdk;

import software.amazon.awscdk.core.Construct;
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

public class GatlingEcrStack extends Stack {
    private final String accountId;

    public GatlingEcrStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.accountId = props.getEnv().getAccount();

        LifecycleRule lifecycleRule = LifecycleRule.builder()
                .rulePriority(1)
                .description("Only keep the last 5 images")
                .maxImageCount(5)
                .tagStatus(TagStatus.ANY)
                .build();

        List<LifecycleRule> lifecycleRules = Collections.singletonList(lifecycleRule);

        buildEcrRepository("EcrRepositoryGatlingRunner", "gatling/gatling-runner", lifecycleRules);
        buildEcrRepository("EcrRepositoryGrafana", "gatling/grafana", lifecycleRules);
        buildEcrRepository("EcrRepositoryInfluxDB", "gatling/influxdb", lifecycleRules);
    }

    private void buildEcrRepository(String id, String repositoryName, List<LifecycleRule> lifecycleRules) {
        Repository ecrRepository = Repository.Builder.create(this, id)
                .repositoryName(repositoryName)
                .lifecycleRules(lifecycleRules)
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
        policyStatement.setSid("AllowAccessFromDevAccount");

        ecrRepository.addToResourcePolicy(policyStatement);
    }
}
