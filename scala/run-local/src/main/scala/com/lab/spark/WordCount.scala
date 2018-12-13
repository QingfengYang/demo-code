package com.lab.spark

import org.apache.spark.{SparkConf, SparkContext}

object WordCount {
  def main(args: Array[String]) {
    val conf = new SparkConf()
      .setMaster("local")
      .setAppName("Word Count")
      .setSparkHome("src/main/resources")
    val sc = new SparkContext(conf)
    val input = sc.textFile("src/main/resources/input.txt")
    val count = input.flatMap(line ⇒ line.split(" "))
      .map(word ⇒ (word, 1))
      .reduceByKey(_ + _)
    count.saveAsTextFile("src/main/resources/outfile")
    println("OK")
  }
}
