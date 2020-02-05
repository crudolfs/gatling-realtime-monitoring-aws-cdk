package com.rudolfs.gatling.cdk.ecs;

import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ecs.ICluster;
import software.amazon.awscdk.services.iam.Role;

public interface GatlingFargateServiceProps {
    static Builder builder() {
        return new Builder();
    }

    String getServiceName();

    String getClusterNamespace();

    IVpc getVpc();

    ICluster getEcsCluster();

    Role getFargateExecutionRole();

    Role getFargateTaskRole();

    StackProps getStackProps();

    class Builder {
        private String serviceName;
        private String clusterNamespace;
        private IVpc vpc;
        private ICluster ecsCluster;
        private Role fargateExecutionRole;
        private Role fargateTaskRole;
        private StackProps stackProps;

        public Builder serviceName(final String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder clusterNamespace(final String clusterNamespace) {
            this.clusterNamespace = clusterNamespace;
            return this;
        }

        public Builder vpc(final IVpc vpc) {
            this.vpc = vpc;
            return this;
        }

        public Builder ecsCluster(final ICluster cluster) {
            this.ecsCluster = cluster;
            return this;
        }

        public Builder fargateExecutionRole(final Role role) {
            this.fargateExecutionRole = role;
            return this;
        }

        public Builder fargateTaskRole(final Role role) {
            this.fargateTaskRole = role;
            return this;
        }

        public Builder stackProps(final StackProps stackProps) {
            this.stackProps = stackProps;
            return this;
        }

        GatlingFargateServiceProps build() {
            return new GatlingFargateServiceProps() {
                @Override
                public String getServiceName() {
                    return serviceName;
                }

                @Override
                public String getClusterNamespace() {
                    return clusterNamespace;
                }

                @Override
                public IVpc getVpc() {
                    return vpc;
                }

                @Override
                public ICluster getEcsCluster() {
                    return ecsCluster;
                }

                @Override
                public Role getFargateExecutionRole() {
                    return fargateExecutionRole;
                }

                @Override
                public Role getFargateTaskRole() {
                    return fargateTaskRole;
                }

                @Override
                public StackProps getStackProps() {
                    return stackProps;
                }
            };
        }

    }
}
