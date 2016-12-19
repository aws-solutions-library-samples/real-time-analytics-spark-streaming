package com.analytics.spark.streaming

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming._


/*
 *  Application to process a data from Kinesis Stream and write the processed data to s3/hdfs.
 */
object KinesisSparkStreaming {

  def main(args: Array[String]): Unit = {

    // Check that all required args were passed in.
    if (args.length != 4) {
      System.err.println(
        """
          |Usage: KinesisSparkStreaming <app-name> <stream-name> <region-name> <output-location>
          |
          |    <app-name> is the name of the consumer app
          |    <stream-name> is the name of the Kinesis stream
          |    <region-name> is the region of the Kinesis service
          |                   (e.g. us-east-1)
        """.stripMargin)
      System.exit(1)
    }

    val Array(appName,streamName,regionName,outputLoc) = args

    val checkpointInterval = Seconds(1)
    val batchInterval = Seconds(1)
    val master = "local[4]"
    val initialPosition = InitialPositionInStream.LATEST
    val hdfsOutput = outputLoc
    val s3Bucket = outputLoc

    val streamingConfig = StreamingConfig(streamName,regionName,checkpointInterval,initialPosition,StorageLevel.MEMORY_AND_DISK_2,appName,master,batchInterval,awsProfile = "default",hdfsOutput,s3Bucket,checkpointDirectory = s"hdfs:///$outputLoc/checkpointDirectory")

    StreamingAnalyzer.execute(streamingConfig)

  }
}
