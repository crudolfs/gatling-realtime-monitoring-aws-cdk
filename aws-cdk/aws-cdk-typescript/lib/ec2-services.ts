import * as cdk from "@aws-cdk/core";
import {
  SecurityGroup,
  Peer,
  Port,
} from "@aws-cdk/aws-ec2";
import {
  ContainerImage,
  LogDriver,
  Volume,
  Scope,
  Ec2TaskDefinition,
  NetworkMode,
  Ec2Service,
} from "@aws-cdk/aws-ecs";
import { DnsRecordType } from "@aws-cdk/aws-servicediscovery";
import { GatlingEcsServiceProps } from "./gatling-ecs-stack";
import { DockerImageAsset } from "@aws-cdk/aws-ecr-assets";
import { LogGroup, RetentionDays } from "@aws-cdk/aws-logs";
import { RemovalPolicy } from "@aws-cdk/core";

export class InfluxdbEc2Service extends cdk.Construct {
  private static readonly INFLUXDB_DATA_VOLUME_NAME: string = "influxdb-data";
  private static readonly INFLUXDB_DATA_VOLUME_CONTAINER_PATH: string = "/var/lib/influxdb";

  constructor(scope: cdk.Construct, id: string, serviceProps: GatlingEcsServiceProps) {
    super(scope, id);

    const securityGroup: SecurityGroup = new SecurityGroup(this, "InfluxdbSecurityGroup", {
        vpc: serviceProps.vpc,
        description: `${serviceProps.serviceName} security group`
      }
    );
    securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8086), "The default port that runs the InfluxDB HTTP service.");
    securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(2003), "The default port that runs the Graphite service.");

    // use RexRay (Docker) volume driver to orchestrate EBS volume attachment to EC2 instances in the cluster
    const dataVolume: Volume = {
      name: this.dataVolumeName(serviceProps.clusterNamespace),
      dockerVolumeConfiguration: {
        autoprovision: true,
        driver: "rexray/ebs",
        driverOpts: {
          "volumetype": "gp2",
          "size": "20"
        },
        scope: Scope.SHARED
      }
    }

    const taskDefinition: Ec2TaskDefinition = new Ec2TaskDefinition(this, "InfluxdbTaskDefinition", {
      executionRole: serviceProps.fargateExecutionRole,
      taskRole: serviceProps.fargateTaskRole,
      networkMode: NetworkMode.AWS_VPC,
      volumes: [dataVolume]
    });

    const influxdbAsset: DockerImageAsset = new DockerImageAsset(this, "influxdbAsset", {
      directory: "../../gatling-monitoring/influxdb"
    });

    taskDefinition.addContainer(serviceProps.serviceName, {
      image: ContainerImage.fromDockerImageAsset(influxdbAsset),
      memoryReservationMiB: 3896,
      cpu: 2048,
      logging: LogDriver.awsLogs({
        logGroup: new LogGroup(this, "influxdbLogGroup", {
          logGroupName: `/ecs/${serviceProps.clusterNamespace}/${serviceProps.serviceName}`,
          retention: RetentionDays.TWO_WEEKS,
          removalPolicy: RemovalPolicy.DESTROY
        }),
        streamPrefix: serviceProps.serviceName
      })
    })
    .addMountPoints({
       sourceVolume: this.dataVolumeName(serviceProps.clusterNamespace),
       containerPath: InfluxdbEc2Service.INFLUXDB_DATA_VOLUME_CONTAINER_PATH,
       readOnly: false
     });

     new Ec2Service(this, id, {
       serviceName: serviceProps.serviceName,
       taskDefinition: taskDefinition,
       desiredCount: 0,
       cloudMapOptions: {
         cloudMapNamespace: serviceProps.ecsCluster.defaultCloudMapNamespace,
         dnsRecordType: DnsRecordType.A,
         name: serviceProps.serviceName
       },
      cluster: serviceProps.ecsCluster,
      securityGroup: securityGroup
     })
  }

  private dataVolumeName(namespace: string): string {
    return `${namespace}-${InfluxdbEc2Service.INFLUXDB_DATA_VOLUME_NAME}`;
  }
}