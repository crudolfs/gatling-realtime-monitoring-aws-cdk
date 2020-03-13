package com.rudolfs.gatling.cdk.ecs;

import com.rudolfs.gatling.cdk.StackBuilder;
import software.amazon.awscdk.core.Stack;
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
    private final String repositoryNamespace;
    private final String gatlingRunnerRepositoryName;
    private final String grafanaRepositoryName;
    private final String influxDBRepositoryName;

    private GatlingEcrStack(Builder builder) {
        super(builder.getScope(), builder.getId(), builder.getStackProps());

        this.accountId = builder.getStackProps().getEnv().getAccount();

        this.repositoryNamespace = builder.repositoryNamespace;
        this.gatlingRunnerRepositoryName = builder.gatlingRunnerRepositoryName;
        this.grafanaRepositoryName = builder.grafanaRepositoryName;
        this.influxDBRepositoryName = builder.influxDBRepositoryName;

        LifecycleRule lifecycleRule = LifecycleRule.builder()
                .rulePriority(1)
                .description("Only keep the last 5 images")
                .maxImageCount(5)
                .tagStatus(TagStatus.ANY)
                .build();

        List<LifecycleRule> lifecycleRules = Collections.singletonList(lifecycleRule);

        buildEcrRepository("EcrRepositoryGatlingRunner", getGatlingRunnerRepositoryName(), lifecycleRules);
        buildEcrRepository("EcrRepositoryGrafana", getGrafanaRepositoryName(), lifecycleRules);
        buildEcrRepository("EcrRepositoryInfluxDB", getInfluxDBRepositoryName(), lifecycleRules);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getGatlingRunnerRepositoryName() {
        return getRepositoryName(this.gatlingRunnerRepositoryName);
    }

    public String getGrafanaRepositoryName() {
        return getRepositoryName(this.grafanaRepositoryName);
    }

    public String getInfluxDBRepositoryName() {
        return getRepositoryName(this.influxDBRepositoryName);
    }

    private String getRepositoryName(String repositoryName) {
        return this.repositoryNamespace + "/" + repositoryName;
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
        policyStatement.setSid("AllowAccessFromAccount");

        ecrRepository.addToResourcePolicy(policyStatement);
    }

    public static final class Builder extends StackBuilder<Builder> {
        private String repositoryNamespace;
        private String gatlingRunnerRepositoryName;
        private String grafanaRepositoryName;
        private String influxDBRepositoryName;

        public Builder repositoryNamespace(String repositoryNamespace) {
            this.repositoryNamespace = repositoryNamespace;
            return this;
        }

        public Builder gatlingRunnerRepositoryName(String repositoryName) {
            this.gatlingRunnerRepositoryName = repositoryName;
            return this;
        }

        public Builder grafanaRepositoryName(String repositoryName) {
            this.grafanaRepositoryName = repositoryName;
            return this;
        }

        public Builder influxDBRepositoryName(String repositoryName) {
            this.influxDBRepositoryName = repositoryName;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public GatlingEcrStack build() {
            return new GatlingEcrStack(this);
        }
    }
}
