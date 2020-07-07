import * as cdk from "@aws-cdk/core";
import {
  SecurityGroup,
  Peer,
  Port,
} from "@aws-cdk/aws-ec2";
import {
  FargateTaskDefinition,
  FargateService,
  ContainerDefinitionOptions,
  ContainerImage,
  LogDriver,
} from "@aws-cdk/aws-ecs";
import { DnsRecordType } from "@aws-cdk/aws-servicediscovery";
import { GatlingEcsServiceProps } from "./gatling-ecs-stack";
import { DockerImageAsset } from "@aws-cdk/aws-ecr-assets";
import { LogGroup, RetentionDays } from "@aws-cdk/aws-logs";
import { RemovalPolicy } from "@aws-cdk/core";

export interface GatlingFargateServiceProps {
  readonly gatlingEcsServiceProps: GatlingEcsServiceProps;
  readonly influxdbHostname: string;
}

export class GrafanaFargateService extends cdk.Construct {
  constructor(scope: cdk.Construct, id: string, serviceProps: GatlingFargateServiceProps) {
    super(scope, id);

    const securityGroup: SecurityGroup = new SecurityGroup(this, "GrafanaSecurityGroup", {
        vpc: serviceProps.gatlingEcsServiceProps.vpc,
        description: `${serviceProps.gatlingEcsServiceProps.serviceName} security group`
      }
    );
    securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3000), "The default port that runs the Grafana UI.");

    const fargateTaskDefinition = new FargateTaskDefinition(this, "GrafanaTaskDefinition", {
        cpu: 1024,
        memoryLimitMiB: 2048,
        executionRole:
          serviceProps.gatlingEcsServiceProps
            .fargateExecutionRole,
        taskRole:
          serviceProps.gatlingEcsServiceProps.fargateTaskRole,
      }
    );

    const grafanaContainerDefinitionOptions = new GrafanaContainerOptions(this, "GrafanaContainerOptions", serviceProps);
    fargateTaskDefinition.addContainer(serviceProps.gatlingEcsServiceProps.serviceName, grafanaContainerDefinitionOptions.containerDefinitionOptions);

    new FargateService(this, id, {
      serviceName: serviceProps.gatlingEcsServiceProps.serviceName,
      taskDefinition: fargateTaskDefinition,
      desiredCount: 0,
      cloudMapOptions: {
        cloudMapNamespace: serviceProps.gatlingEcsServiceProps.ecsCluster.defaultCloudMapNamespace,
        dnsRecordType: DnsRecordType.A,
        name: serviceProps.gatlingEcsServiceProps.serviceName,
      },
      cluster: serviceProps.gatlingEcsServiceProps.ecsCluster,
      securityGroup: securityGroup,
      assignPublicIp: true,
      vpcSubnets: {
        subnets: serviceProps.gatlingEcsServiceProps.vpc.publicSubnets,
      },
    });
  }
}

export class GatlingRunnerFargateService extends cdk.Construct {
  constructor(scope: cdk.Construct, id: string, serviceProps: GatlingFargateServiceProps) {
    super(scope, id);

    const securityGroup: SecurityGroup = new SecurityGroup(this, "GatlingRunnerSecurityGroup", {
        vpc: serviceProps.gatlingEcsServiceProps.vpc,
        description: `${serviceProps.gatlingEcsServiceProps.serviceName} security group`
      }
    );

    const fargateTaskDefinition = new FargateTaskDefinition(this, "GatlingRunnerFargateTaskDefinition", {
        cpu: 1024,
        memoryLimitMiB: 2048,
        executionRole:
          serviceProps.gatlingEcsServiceProps.fargateExecutionRole,
        taskRole:
          serviceProps.gatlingEcsServiceProps.fargateTaskRole,
      }
    );

    const gatlingRunnerAsset = new DockerImageAsset(this, "gatlingRunnerAsset", {
      directory: "../../gatling-monitoring/gatling-runner"
    });

    fargateTaskDefinition.addContainer(serviceProps.gatlingEcsServiceProps.serviceName, {
      image: ContainerImage.fromDockerImageAsset(gatlingRunnerAsset),
      command: ["-gh", serviceProps.influxdbHostname],
      logging: LogDriver.awsLogs({
        logGroup: new LogGroup(this, "gatlingRunnerFargateLogGroup", {
          logGroupName: `/ecs/${serviceProps.gatlingEcsServiceProps.clusterNamespace}/${serviceProps.gatlingEcsServiceProps.serviceName}`,
          retention: RetentionDays.TWO_WEEKS,
          removalPolicy: RemovalPolicy.DESTROY
        }),
        streamPrefix: serviceProps.gatlingEcsServiceProps.serviceName
      })
    });

    new FargateService(this, id, {
      serviceName: serviceProps.gatlingEcsServiceProps.serviceName,
      taskDefinition: fargateTaskDefinition,
      desiredCount: 0,
      cluster: serviceProps.gatlingEcsServiceProps.ecsCluster,
      securityGroup: securityGroup,
      assignPublicIp: true,
      vpcSubnets: {
        subnets: serviceProps.gatlingEcsServiceProps.vpc.privateSubnets,
      },
    });
  }
}

class GrafanaContainerOptions extends cdk.Construct {
  private _containerDefinitionOptions: ContainerDefinitionOptions;

  constructor(scope: cdk.Construct, id: string, grafanaFargateServiceProps: GatlingFargateServiceProps) {
    super(scope, id);

    const environmentVariables = {
      "INFLUXDB_ACCESS_MODE": "proxy",
      "INFLUXDB_HOST": grafanaFargateServiceProps.influxdbHostname,
      "INFLUXDB_PORT": "8086"
    };

    const grafanaAsset = new DockerImageAsset(this, "grafanaAsset", {
      directory: "../../gatling-monitoring/grafana"
    });

    this._containerDefinitionOptions = {
      image: ContainerImage.fromDockerImageAsset(grafanaAsset),
      logging: LogDriver.awsLogs({
        logGroup: new LogGroup(this, "grafanaFargateLogGroup", {
          logGroupName: `/ecs/${grafanaFargateServiceProps.gatlingEcsServiceProps.clusterNamespace}/${grafanaFargateServiceProps.gatlingEcsServiceProps.serviceName}`,
          retention: RetentionDays.TWO_WEEKS,
          removalPolicy: RemovalPolicy.DESTROY
        }),
        streamPrefix: grafanaFargateServiceProps.gatlingEcsServiceProps.serviceName
      }),
      environment: environmentVariables
    };
  }

  get containerDefinitionOptions() {
    return this._containerDefinitionOptions;
  }
}