package com.analytics.spark.streaming

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.Duration

/**
  * StreamingConfig with parameters for Kinesis.
  */
case class StreamingConfig(

  streamName:         String,
  region:             String,
  checkpointInterval: Duration,
  initialPosition:    InitialPositionInStream,
  storageLevel:       StorageLevel,
  appName:            String,
  master:             String,
  batchInterval:      Duration,
  awsProfile:         String,
  hdfsOutputLoc:      String   ,
  s3OutputLoc:        String,
  checkpointDirectory: String

) {
  /**
    * The Kinesis endpoint from the region.
    */
  val endpointUrl = s"https://kinesis.${region}.amazonaws.com"
}
