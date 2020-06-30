/*********************************************************************************************************************
#  Copyright 2019-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.                                      #
#                                                                                                                    #
#  Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance    #
#  with the License. A copy of the License is located at                                                             #
#                                                                                                                    #
#      http://www.apache.org/licenses/LICENSE-2.0                                                                    #
#                                                                                                                    #
#  or in the 'license' file accompanying this file. This file is distributed on an 'AS IS' BASIS, WITHOUT WARRANTIES #
#  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions    #
#  and limitations under the License.                                                                                #
*********************************************************************************************************************/

package com.demo.consumer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import org.apache.spark.streaming.kinesis.KinesisInitialPositions;
import org.apache.spark.streaming.kinesis.KinesisInputDStream;
import scala.reflect.ClassTag$;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;

import com.demo.model.Record;

public class KinesisConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisConsumer.class);
    private static final String DELIMITER = ",";

    public static void main(String[] args) throws Exception {
        // Check that all required args were passed in
        if (args.length != 4) {
            System.err.println("Usage: KinesisConsumer <app-name> <stream-name> <region-name> <output-location>\n\n"
                + "    <app-name> is the name of the app, used to track the read data in DynamoDB\n"
                + "    <stream-name> is the name of the Kinesis stream\n"
                + "    <region-name> region where the Kinesis stream is created\n"
                + "    <output-location> bucket on S3 where the data should be stored.\n");
            System.exit(1);
        }

        // Populate the appropriate variables from the given args
        String kinesisAppName = args[0];
        String streamName = args[1];
        String regionName = args[2];
        String outputLocation = args[3];

        String endpointURL = "https://kinesis." + regionName + ".amazonaws.com";
        LOGGER.info("EndpointURL is " + endpointURL);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/HH/mm");

        // Create a Kinesis client in order to determine the number of shards for the given stream
        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard()
            .withEndpointConfiguration(new EndpointConfiguration(endpointURL, regionName))
            .withCredentials(DefaultAWSCredentialsProviderChain.getInstance());

        AmazonKinesis kinesis = clientBuilder.build();
        int numShards = kinesis.describeStream(streamName).getStreamDescription().getShards().size();
        int numStreams = numShards;
        LOGGER.info("Number of shards is " + numShards);

        // Spark Streaming batch interval
        Duration batchInterval = Durations.minutes(1);

        // Kinesis checkpoint interval. Same as batchInterval for this example.
        Duration kinesisCheckpointInterval = batchInterval;

        // Setup the Spark config and StreamingContext
        SparkConf sparkConfig = new SparkConf().setAppName(kinesisAppName);
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConfig, batchInterval);

        List<JavaDStream<byte[]>> streamsList = new ArrayList<>(numStreams);
        for (int i = 0; i < numStreams; i++) {
            streamsList.add(JavaDStream.fromDStream(
                KinesisInputDStream.builder()
                    .streamingContext(jssc)
                    .checkpointAppName(kinesisAppName)
                    .streamName(streamName)
                    .endpointUrl(endpointURL)
                    .regionName(regionName)
                    .initialPosition(new KinesisInitialPositions.Latest())
                    .checkpointInterval(kinesisCheckpointInterval)
                    .storageLevel(StorageLevel.MEMORY_AND_DISK_2())
                    .build(),
                ClassTag$.MODULE$.apply(byte[].class)
            ));
        }

        JavaDStream<byte[]> unionStreams;
        if (streamsList.size() > 1) {
            // Union all the streams if there is more than 1 stream
            LOGGER.info("Stream size is greater than 1");
            unionStreams = jssc.union(streamsList.get(0), streamsList.subList(1, streamsList.size()));
        } else {
            // Otherwise, just use the 1 stream
            LOGGER.info("Stream size is equal to 1");
            unionStreams = streamsList.get(0);
        }

        // Convert each line of Array[Byte] to String
        JavaDStream<String> items = unionStreams.flatMap(new FlatMapFunction<byte[], String>() {
            @Override
            public Iterator<String> call(byte[] line) {
                String s = new String(line, StandardCharsets.UTF_8);
                return Arrays.asList(s.split("\n")).iterator();
            }
        });

        // Convert RDDs of the items DStream to DataFrame and run SQL query
        items.window(Durations.minutes(1)).foreachRDD((rdd, time) -> {
            LOGGER.info("========= Time is " + time + " =========");
            if (rdd.count() > 0) {
                String outPartitionFolder = sdf.format(time.milliseconds());
                SparkSession spark = JavaSparkSessionSingleton.getInstance(rdd.context().getConf());

                // Convert JavaRDD[String] to JavaRDD[Record]
                JavaRDD<Record> rowRDD = rdd.map(line -> {
                    String[] parts = line.split(DELIMITER);

                    Record record = new Record();
                    record.setZipcode(Integer.parseInt(parts[0]));
                    record.setProductName(parts[1]);
                    record.setPrice(Integer.parseInt(parts[2]));
                    record.setTimestamp(parts[3]);
                    return record;
                });

                // Creates a temporary view using the DataFrame
                Dataset<Row> recordsDataFrame = spark.createDataFrame(rowRDD, Record.class);
                recordsDataFrame.createOrReplaceTempView("records");

                // Query table using SQL and save results to S3
                // The coalesce method will make sure only one file is generated on outPartitionFolder
                // For larger datasets, you might use javaRDD().saveAsTextFile(...) so that multiple files are used
                Dataset<Row> products = spark.sql("SELECT productName, price FROM records WHERE price >= 40 AND price <= 50");
                products.javaRDD().coalesce(1).saveAsTextFile("s3://" + outputLocation + "/historical/" + outPartitionFolder, GzipCodec.class);
            }
        });

        // Start the streaming context and await termination
        jssc.start();
        jssc.awaitTermination();
    }
}

class JavaSparkSessionSingleton {
    private static volatile SparkSession instance = null;

    private JavaSparkSessionSingleton() { }

    public static SparkSession getInstance(SparkConf sparkConf) {
        if (instance == null) {
            synchronized (JavaSparkSessionSingleton.class) {
                if (instance == null) {
                    instance = SparkSession.builder().config(sparkConf).getOrCreate();
                }
            }
        }

        return instance;
    }
}
