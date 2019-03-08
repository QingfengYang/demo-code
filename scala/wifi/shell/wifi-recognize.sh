#!/usr/bin/env bash

PRGDIR=$(dirname "$0")
BASE_DIR=$(cd "$PRGDIR"/.. > /dev/null; pwd)
PARAM_DATE=$1
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
# for test hadoop env
export SPARK_HOME=/www/uprofile-calculate-engine/spark-2.1.0-bin-hadoop2.7
#export SPARK_CLASSPATH=/www/hadoop/lib:$SPARK_CLASSPATH
export PATH=$SPARK_HOME/bin:$PATH
spark-submit \
    --class com.meitu.userprofile.feature.engineering.WifiRecognizeApp \
    --master yarn \
    --num-executors 30 \
    --executor-memory 7G \
    --driver-memory 1G \
    --deploy-mode client \
    --queue root.other.temp \
    --driver-java-options "-Dlog4j.configuration=file:/www/uprofile-etl-project/log4j-spark.properties -Dvm.logging.level=DEBUG" \
    --files /www/uprofile-etl-project/log4j-spark.properties \
    --jars $HADOOP_HOME/lib/hadoop-lzo-0.4.20-SNAPSHOT.jar \
    $BASE_DIR/wifi-latest-jar-with-dependencies.jar $PARAM_DATE user_profile.uprofile_oda_wifi_stat_geo_v1 user_profile.uprofile_oda_wifi_stat_geo_cluster_v1 30
