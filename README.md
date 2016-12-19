
# Real-Time Analytics with Spark Streaming

A solution describing data-processing design pattern for streaming data through Kinesis and Spark Streaming at real-time.

## Overview

In today’s world, you need to continually innovate and improve your product to meet your customers’ needs. Require to provide an analytic ecosystem that scales with the business needs. Need to provide options in New Analytics Technologies by focusing on enabling new analytic approaches including programmatic Data Analysis. Analytics with kinesis and spark streaming helps to reliably process real-time data.

Processing vast amount of semi-structured data is a challenge between at what instance the data is collected and it availability for presentation. The common delay is due to requirement to validate or identify the coarse data. Often able to react immediately to new data is significant. AWS tool like EMR and Kinesis is used to process semi-structured or unstructured data from data streams at real-time. The real-time data processing is achieved with AWS Services such as Amazon Kinesis, Amazon EMR (Spark), Amazon S3. 

Spark is an in-memory processing engine which can process vast data in fraction of seconds. Spark streaming, Spark SQL leverages the ability to process batch data with real-time data. Spark 2.0 has a number of benefits with the new Dataset API

•	Static-typing and runtime type-safety<br />
•	High-level abstraction and custom view into structured and semi-structured data<br /> 
•	Ease-of-use of APIs with structure<br />
•	Performance and Optimization<br />

When processing large amounts of semi-structured data, there is always a delay between the point when data is collected and its availability in dashboards. Often the delay results from the need to validate or at least identify coarse data. In some cases, however, being able to react immediately to new data is more important than being 100 percent certain of the data’s validity. 

The AWS tool most frequently used to deal with large volumes of semi-structured or unstructured data is Amazon Elastic MapReduce (Amazon EMR). Stream or real-time processing, the processing of a constant flux of data, in real time, is possible with a Lambda Architecture solution that includes Amazon Kinesis, Amazon Simple Storage Service (Amazon S3), Spark Streaming, and Spark SQL on top of an Amazon EMR cluster.

## Abstract

The solution focuses on functional areas such as real-time data from Kinesis is processed with spark streaming and data visualization is done with Zeppelin.
The solution supports three options in spark submit mode 

•	Deploy a default DemoApp application.<br />
•	Deploying a custom spark application for real-time streaming on an EMR cluster.<br />
•	Visualize the real-time data and process the spark application code from Zeppelin UI.<br />

This design deploys a spark application written in high-level programming language Scala on a EMR cluster in an Amazon VPC, which is a logically isolated section of the Amazon Web Services (AWS). All the components of EMR with the spark application are deployed into the VPC and within a private subnet. The EMR web Interfaces are enabled by Port forwarding from a bastion host which internally forwards to the EMR master node.

## Cloudformation Templates

•	vpc-with-private-public-subnets.template<br />
•	producer.template<br />
•	realtime-analytics.template<br />

***

Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Amazon Software License (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

    http://aws.amazon.com/asl/

or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions and limitations under the License.





