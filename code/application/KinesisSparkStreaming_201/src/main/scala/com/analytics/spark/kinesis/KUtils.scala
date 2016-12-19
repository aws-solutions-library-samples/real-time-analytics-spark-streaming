package com.analytics.spark.kinesis

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.AmazonKinesisClient

/**
  * Kinesis Utility for ShardCounts and Client Connection
  */
object KUtils {

  /**
    * Singleton Object that takes 1.KCL/AWS credentials object. 2.Kinesis Stream
    * name. The utility function queries the stream name and determines how many
    * shards are in the stream so that the sharded data can be unioned/joined
    * for processing by Apache Spark Streaming application
    *
    */
  def getShardCount(kinesisClient: AmazonKinesisClient, stream: String): Int =
  kinesisClient
    .describeStream(stream)
    .getStreamDescription
    .getShards
    .size

  /**
    * Finds AWS Credential by provided awsProfile and creates Kinesis Client
    */
  def setupKinesisClientConnection(endpointUrl: String, awsProfile: String): AmazonKinesisClient = {

    val credentials = new DefaultAWSCredentialsProviderChain().getCredentials

    require(credentials != null,
      "No AWS credentials found. Please specify credentials using one of the methods specified " +
        "in http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html")

    val akc = new AmazonKinesisClient(credentials)
    akc.setEndpoint(endpointUrl)
    akc
  }

}
