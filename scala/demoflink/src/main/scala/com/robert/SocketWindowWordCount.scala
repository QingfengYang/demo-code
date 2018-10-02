package com.robert

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.scala._

object SocketWindowWordCount {

  def main(args: Array[String]): Unit = {

    var hostname: String = "localhost"
    var port: Int = 0

    try {
      val params = ParameterTool.fromArgs(args)
      hostname = if (params.has("hostname")) params.get("hostname") else "localhost"
      port = params.getInt("port")

    } catch {
      case e: Exception => {
        System.err.println("Not port specified")
        return
      }
    }

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val text: DataStream[String] = env.socketTextStream(hostname, port, '\n')

    val wordCounts = text.flatMap(w => w.split("\\s")).map {
      w: String => WordWithCount(w, 1)
    }.keyBy("word").timeWindow(Time.seconds(3)).sum("count")

    wordCounts.print().setParallelism(1)
    env.execute("Socket window WordCount")
  }

  case class WordWithCount(word: String, count: Int)
}