# Real-Time Analytics with Spark Streaming

A solution describing data-processing design pattern for streaming data through Kinesis and Spark Streaming at real-time.

## Overview

In today’s world, you need to continually innovate and improve your product to meet your customers’ needs. Require to provide an analytic ecosystem that scales with the business needs. Need to provide options in New Analytics Technologies by focusing on enabling new analytic approaches including programmatic Data Analysis. Analytics with kinesis and spark streaming helps to reliably process real-time data.

Processing vast amount of semi-structured data is a challenge between at what instance the data is collected and it availability for presentation. The common delay is due to requirement to validate or identify the coarse data. Often able to react immediately to new data is significant. AWS tool like EMR and Kinesis is used to process semi-structured or unstructured data from data streams at real-time. The real-time data processing is achieved with AWS Services such as Amazon Kinesis, Amazon EMR (Spark), Amazon S3.

When processing large amounts of semi-structured data, there is always a delay between the point when data is collected and its availability in dashboards. Often the delay results from the need to validate or at least identify coarse data. In some cases, however, being able to react immediately to new data is more important than being 100 percent certain of the data’s validity.

The AWS tool most frequently used to deal with large volumes of semi-structured or unstructured data is Amazon Elastic MapReduce (Amazon EMR). Stream or real-time processing, the processing of a constant flux of data, in real time, is possible with a Lambda Architecture solution that includes Amazon Kinesis, Amazon Simple Storage Service (Amazon S3), Spark Streaming, and Spark SQL on top of an Amazon EMR cluster.

## Abstract

The solution focuses on functional areas such as real-time data from Kinesis is processed with spark streaming and data visualization is done with Zeppelin.

All the components of EMR are deployed into the VPC and within a private subnet. The EMR web Interfaces are enabled by Port forwarding from a bastion host which internally forwards to the EMR master node.

## Cloudformation Templates
* aws-vpc.template
* real-time-analytics-spark-streaming.template

## Collection of operational metrics
This solution collects anonymous operational metrics to help AWS improve the quality of features of the solution.
For more information, including how to disable this capability, please see the [implementation guide](https://docs.aws.amazon.com/solutions/latest/real-time-analytics-spark-streaming/appendix-b.html).

# Known issues

## EMR security groups
When deleting the solution stack, the Amazon EMR cluster does not delete the following security groups (since they reference one another): _ElasticMapReduce-Slave-Private_, _ElasticMapReduce-ServiceAccess_, and _ElasticMapReduce-Master-Private_. This will cause the VPC stack deletion to fail, since a VPC cannot be deleted while there are still available security groups.

To remove all inbound and outbound rules for a security group, use the AWS Command Line Interface (AWS CLI) and enter the following commands, which must be executed for each group listed above. Using this method requires [jq](https://stedolan.github.io/jq/), a lightweight command-line JSON processor, to be installed.

```console
SECURITY_GROUP_ID=<REPLACE_ME>
aws ec2 describe-security-groups --group-ids $SECURITY_GROUP_ID --output json | jq '.SecurityGroups[0].IpPermissions' > IpPermissions.json
aws ec2 describe-security-groups --group-ids $SECURITY_GROUP_ID --output json | jq '.SecurityGroups[0].IpPermissionsEgress' > IpPermissionsEgress.json
aws ec2 revoke-security-group-ingress --group-id $SECURITY_GROUP_ID --ip-permissions file://IpPermissions.json
aws ec2 revoke-security-group-egress --group-id $SECURITY_GROUP_ID --ip-permissions file://IpPermissionsEgress.json
```

Once all rules have been removed, the security group can be deleted:

```console
aws ec2 delete-security-group --group-id $SECURITY_GROUP_ID
```

## Spark Security
In Apache Spark, security is OFF by default (for more details, check this [GitHub Advisory](https://github.com/advisories/GHSA-phg2-9c5g-m4q7)). To mitigate this, the solution leverages network-level restrictions (more specifically, private subnets and security groups that only allow access from a bastion host) to secure the cluster. These restrictions are highlighted below, with links to the corresponding sections from the CloudFormation templates.

[VPC](deployment/aws-vpc.template#L253-L265):
```yaml
  PrivateSubnet1A:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref 'VPC'
      CidrBlock: !Ref 'PrivateSubnet1ACIDR'
      AvailabilityZone: !Select
        - '0'
        - !Ref 'AvailabilityZones'
      Tags:
        - Key: Name
          Value: Private subnet 1A
        - Key: Network
          Value: Private

  PrivateSubnet1ARoute:
    Type: AWS::EC2::Route
    Properties:
      RouteTableId: !Ref 'PrivateSubnet1ARouteTable'
      DestinationCidrBlock: '0.0.0.0/0'
      NatGatewayId: !Ref 'NATGateway1'
```

[EMR Cluster](deployment/real-time-analytics-spark-streaming.template#L1100):
```yaml
  EMRCluster:
    Type: AWS::EMR::Cluster
    Properties:
      Applications:
        - Name: Hadoop
        - Name: Hive
        - Name: Spark
        - Name: Zeppelin
        - Name: Hue
      Instances:
        Ec2SubnetId: !GetAtt 'VPCStackQ.Outputs.PrivateSubnet1AID'
```

***

Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
