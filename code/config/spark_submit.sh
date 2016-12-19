#!/bin/bash
set -x -e

#Run only on master
if grep isMaster /mnt/var/lib/info/instance.json | grep true;
then
    /usr/lib/spark/bin/spark-submit --deploy-mode cluster --class com.analytics.spark.streaming.KinesisSparkStreaming --master yarn s3://SparkAppBucket/SparkApplicationJar SparkApplicationName myStream us-east-1 SparkOutputLocation

fi
