import * as cdk from '@aws-cdk/core';
import { IVpc, ISubnet, InstanceType, InstanceClass, InstanceSize, UserData } from '@aws-cdk/aws-ec2';
import { Cluster, EcsOptimizedImage, ICluster } from '@aws-cdk/aws-ecs';
import { NamespaceType } from '@aws-cdk/aws-servicediscovery';
import { Ec2InstanceProfileRole, FargateExecutionRole, FargateTaskRole } from './gatling-ecs-roles';
import { AutoScalingGroup } from '@aws-cdk/aws-autoscaling';
import { Role } from '@aws-cdk/aws-iam';
import { GrafanaFargateService, GatlingRunnerFargateService } from './fargate-services';
import { InfluxdbEc2Service } from './ec2-services';

export interface GatlingEcsStackProps {
  readonly vpc: IVpc;
  readonly namespace: string;
}

export interface GatlingEcsServiceProps {
  readonly vpc: IVpc;
  readonly ecsCluster: ICluster;
  readonly fargateExecutionRole: Role;
  readonly fargateTaskRole: Role;
  readonly serviceName: string;
  readonly clusterNamespace: string;
}

/**
 * Creates the CloudFormation for the AWS ECS Cluster and Services for the Gatling Real-Time Monitoring stack.
 * The Gatling ECS cluster uses Fargate to run stateless services and EC2 to run stateful services (InfluxDB in this case).
 */
export class GatlingEcsStack extends cdk.Stack {

  private static readonly DEFAULT_GATLING_RUNNER_SERVICE_NAME: string = "gatling-runner";
  private static readonly DEFAULT_GRAFANA_SERVICE_NAME: string = "grafana";
  private static readonly DEFAULT_INFLUXDB_SERVICE_NAME: string = "influxdb";
  private static readonly DEFAULT_AVAILABILITY_ZONE_EBS_VOLUME: string = "eu-west-1a";

  constructor(scope: cdk.Construct, id: string, gatlingEcsStackProps: GatlingEcsStackProps, props?: cdk.StackProps) {
    super(scope, id, props);

    const ecsClusterName: string = `${gatlingEcsStackProps?.namespace}-cluster`;

    // ECS Cluster setup
    const cluster = new Cluster(this, 'GatlingCluster', {
      clusterName: ecsClusterName,
      vpc: gatlingEcsStackProps.vpc,
      defaultCloudMapNamespace: {
        name: gatlingEcsStackProps.namespace,
        type: NamespaceType.DNS_PRIVATE,
        vpc: gatlingEcsStackProps.vpc
      }
    });

    // Role used for the EC2 service types, this role contains the correct statements to allow Rexray volume driver to do its work
    const instanceProfileRole = new Ec2InstanceProfileRole(this, "InstanceProfileRole", gatlingEcsStackProps.namespace);

    // EBS volume is used to store data for stateful services. This can only be re-attached within the
    // AvailabilityZone (AZ) where the EBS volume is created. Therefore determine which AZ to use.
    const privateSubnet: ISubnet = gatlingEcsStackProps.vpc.privateSubnets[0];
    const availabilityZone: string = privateSubnet != null ? privateSubnet.availabilityZone : GatlingEcsStack.DEFAULT_AVAILABILITY_ZONE_EBS_VOLUME;

    // AutoScalingGroup needed for the stateful (EC2) services
    const autoScalingGroup = new AutoScalingGroup(this, "AutoScalingGroup", {
    // important to install rexray volume driver and restart ECS agent afterwards
      userData: UserData.custom(`#!/bin/bash\n
       echo ECS_CLUSTER=${ecsClusterName} >> /etc/ecs/ecs.config\n
      "echo ECS_AWSVPC_BLOCK_IMDS=true >> /etc/ecs/ecs.config\n
      "docker plugin install rexray/ebs REXRAY_PREEMPT=true EBS_REGION=${availabilityZone} --grant-all-permissions\n
      "stop ecs\n
      "start ecs`),
    // use t3a.medium
      instanceType: InstanceType.of(InstanceClass.BURSTABLE3_AMD, InstanceSize.MEDIUM),
      machineImage: EcsOptimizedImage.amazonLinux2(),
      role: instanceProfileRole,
      // limitation of EBS volume attachment within same AZ requires to allow EC2 instance creation in 1 AZ (and therefore subnet) only
      vpcSubnets: {
        subnets: [privateSubnet]
      },
      vpc: gatlingEcsStackProps.vpc
    });

    cluster.addAutoScalingGroup(autoScalingGroup);

    // IAM Roles needed to execute AWS ECS Fargate tasks
    const fargateExecutionRole = new FargateExecutionRole(this, "FargateEcsExecutionRole", gatlingEcsStackProps.namespace);
    const fargateTaskRole = new FargateTaskRole(this, "FargateEcsTaskRole", gatlingEcsStackProps.namespace);
    // service discovery endpoint of the InfluxDB service
    const influxdbHostName = `${GatlingEcsStack.DEFAULT_INFLUXDB_SERVICE_NAME}.${gatlingEcsStackProps.namespace}`;

    // Fargate service for Grafana
    new GrafanaFargateService(this, "GrafanaFargateService", {
      influxdbHostname: influxdbHostName,
      gatlingEcsServiceProps: {
        serviceName: GatlingEcsStack.DEFAULT_GRAFANA_SERVICE_NAME,
        clusterNamespace: gatlingEcsStackProps.namespace,
        ecsCluster: cluster,
        fargateExecutionRole: fargateExecutionRole,
        fargateTaskRole: fargateTaskRole,
        vpc: gatlingEcsStackProps.vpc
      }
    });

    // EC2 (stateful) service for InfluxDB
    new InfluxdbEc2Service(this, "InfluxdbEc2Service", {
      serviceName: GatlingEcsStack.DEFAULT_INFLUXDB_SERVICE_NAME,
      clusterNamespace: gatlingEcsStackProps.namespace,
      ecsCluster: cluster,
      fargateExecutionRole: fargateExecutionRole,
      fargateTaskRole: fargateTaskRole,
      vpc: gatlingEcsStackProps.vpc
    });

    // Fargate service for Gatling runner
    new GatlingRunnerFargateService(this, "GatlingRunnerFargateService", {
      influxdbHostname: influxdbHostName,
      gatlingEcsServiceProps: {
        serviceName: GatlingEcsStack.DEFAULT_GATLING_RUNNER_SERVICE_NAME,
        clusterNamespace: gatlingEcsStackProps.namespace,
        ecsCluster: cluster,
        fargateExecutionRole: fargateExecutionRole,
        fargateTaskRole: fargateTaskRole,
        vpc: gatlingEcsStackProps.vpc
      }
    });
  }
}
