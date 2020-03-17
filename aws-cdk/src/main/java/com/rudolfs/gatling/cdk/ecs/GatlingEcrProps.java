package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.services.ecr.RepositoryAttributes;

public interface GatlingEcrProps {
    static Builder builder() {
        return new Builder();
    }

    String getRepositoryNamespace();

    String getGatlingRunnerRepositoryName();

    String getGrafanaRepositoryName();

    String getInfluxDBRepositoryName();

    default String getRepositoryNameWithNamespace(String repositoryName) {
        return getRepositoryNamespace() + "/" + repositoryName;
    }

    default String getRepositoryArn(String region, String account, String repositoryName) {
        return String.format("arn:aws:ecr:%s:%s:repository/%s", region, account, getRepositoryNameWithNamespace(repositoryName));
    }

    default RepositoryAttributes getRepositoryAttributes(String region, String account, String repositoryName) {
        return RepositoryAttributes.builder()
                .repositoryArn(getRepositoryArn(region, account, repositoryName))
                .repositoryName(getRepositoryNameWithNamespace(repositoryName))
                .build();
    }

    class Builder {
        private String repositoryNamespace;
        private String gatlingRunnerRepositoryName;
        private String grafanaRepositoryName;
        private String influxDBRepositoryName;

        public Builder repositoryNamespace(final String repositoryNamespace) {
            this.repositoryNamespace = repositoryNamespace;
            return this;
        }

        public Builder gatlingRunnerRepositoryName(final String gatlingRunnerRepositoryName) {
            this.gatlingRunnerRepositoryName = gatlingRunnerRepositoryName;
            return this;
        }

        public Builder grafanaRepositoryName(final String grafanaRepositoryName) {
            this.grafanaRepositoryName = grafanaRepositoryName;
            return this;
        }

        public Builder influxDBRepositoryName(final String influxDBRepositoryName) {
            this.influxDBRepositoryName = influxDBRepositoryName;
            return this;
        }

        public GatlingEcrProps build() {
            return new GatlingEcrProps() {
                @Override
                public String getRepositoryNamespace() {
                    return repositoryNamespace;
                }

                @Override
                public String getGatlingRunnerRepositoryName() {
                    return gatlingRunnerRepositoryName;
                }

                @Override
                public String getGrafanaRepositoryName() {
                    return grafanaRepositoryName;
                }

                @Override
                public String getInfluxDBRepositoryName() {
                    return influxDBRepositoryName;
                }
            };
        }

    }
}
