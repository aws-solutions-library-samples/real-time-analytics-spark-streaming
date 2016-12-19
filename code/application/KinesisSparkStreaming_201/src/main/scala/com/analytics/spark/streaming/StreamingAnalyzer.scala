package com.analytics.spark.streaming

import java.util.Date

import com.analytics.spark.kinesis.{KUtils => KU}
import org.apache.hadoop.io.compress.GzipCodec
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.streaming.kinesis.KinesisUtils
import org.apache.spark.streaming.{Seconds, StreamingContext, Time}


case class Record(zipcode: String,ProductName: String,price: BigInt,timestamp: String)

/**
  * Streaming Analyzer to analyze and execute streaming data
  */
object StreamingAnalyzer {

  /**
    * Private function to set up Spark Streaming
    *
    * @param config The configuration for our job using StreamingCountsConfig.scala
    */
  private def   setupStreamingContext(sparkConf: SparkConf,config: StreamingConfig): StreamingContext = {
    val ssc = new StreamingContext(sparkConf, config.batchInterval)
    ssc.checkpoint(config.checkpointDirectory)
    ssc
  }


  def execute(config: StreamingConfig){

    // Spark Streaming connection to Kinesis
    val kinesisClient = KU.setupKinesisClientConnection(config.endpointUrl,config.awsProfile)

    require(kinesisClient != null,
      "No AWS credentials found. Please specify credentials using one of the methods specified " +
        "in http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html")

    val sparkConf = new SparkConf().setAppName(config.appName)

    val streamingSparkContext = StreamingContext.getOrCreate(config.checkpointDirectory,
      () => {
        setupStreamingContext(sparkConf,config)
      })
    val spark = SparkSession.builder().config(sparkConf).appName(config.appName).enableHiveSupport().getOrCreate()

    val numShards = KU.getShardCount(kinesisClient, config.streamName)

    val sparkDStreams = (0 until numShards).map { i =>

      KinesisUtils.createStream(
        ssc = streamingSparkContext,
        kinesisAppName = config.appName,
        streamName = config.streamName,
        endpointUrl = config.endpointUrl,
        regionName = config.region,
        initialPositionInStream = config.initialPosition,
        checkpointInterval = config.batchInterval,
        storageLevel = config.storageLevel
      )
    }
    //Union DStreams

    val unionStreams = streamingSparkContext.union(sparkDStreams)

    val items = unionStreams.flatMap(byteArray => new String(byteArray).split("\n"))

    val itemStatus = items.map(status => status.toString())

    /* Setting the date format */
    val outDateFormat =  new java.text.SimpleDateFormat("yyyy/MM/dd/HH/mm")

    items.foreachRDD((rdd:RDD[String],time:Time) => {
      import spark.implicits._

      //If data is present then continue
      if (rdd.count() > 0) {
        try {

          val recordsDS = spark.read.json(rdd).as[Record]
          recordsDS.createOrReplaceTempView("records")
          recordsDS.show()

          val products = spark.sql("SELECT ProductName,price FROM records WHERE price >= 40 AND price <= 50")
          products.createOrReplaceTempView("products")
          products.show()

          val outPartitionFolder = outDateFormat.format(new Date(time.milliseconds))

          products.rdd.coalesce(1).saveAsTextFile("%s/%s".format(s"hdfs:///${config.hdfsOutputLoc}", outPartitionFolder),
            classOf[GzipCodec])

        } catch {
          case e: Exception => println("Empty stream" + e)
        }

      }

    })

    // Generate RDD every 120sec for last 60sec of data and write to S3 for backup
    val batchedStatuses = itemStatus.window(Seconds(60), Seconds(60))
    val coalesced = batchedStatuses.transform(rdd => rdd.coalesce(1))

    coalesced.foreachRDD((rdd:RDD[String],time:Time) =>  {
      val outPartitionFolder = outDateFormat.format(new Date(time.milliseconds))
      rdd.saveAsTextFile("%s/%s".format(s"s3n://${config.s3OutputLoc}/historical/", outPartitionFolder),
        classOf[GzipCodec])

    })

    // Start Spark Streaming process
    streamingSparkContext.start()
    streamingSparkContext.awaitTermination()

  }
}
