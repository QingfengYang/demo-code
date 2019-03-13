package com.meitu.userprofile.feature.engineering

import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.sql.{DataFrame, Dataset, SaveMode, SparkSession}
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable.ArrayBuffer

object WifiRecognizeApp {

  val WIFI_CONNECT_ACTION: String = "wifi_connect_action"
  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      val usage =
        """
          Usage: date, srouce table, target table, debug
        """.stripMargin
      println(usage)
      System.exit(1)
    }
    val datePartition = args(0)
    val sourceHiveTable = args(1)
    val targetHiveTable = args(2)

    val maxIter = if (args.length >= 4) {
      args(3).toInt
    } else {
      6
    }

    val isDebug = if (args.length == 5 && args(4).equalsIgnoreCase("debug")) {
      true
    } else {
      false
    }

    val (spark, wfConnectActAll: Dataset[UidConnectWifi]) = if (isDebug) {
      val spark = SparkSession.builder().master("local[2]").appName("WIFI Recognize Graph Application").getOrCreate()
      import spark.implicits._
      val wfConnectActTmp: Dataset[UidConnectWifi] = spark.sparkContext.textFile(sourceHiveTable).map(line => line.split("\t")).zipWithUniqueId()
        .map{case(parts, vid) =>
          UidConnectWifi(parts(0).trim.toLong, parseStringElement(parts(1)), parseStringElement(parts(2)),
            parseStringElement(parts(3)), parts(4).trim.toInt, parseStringElement(parts(5)), vid)
        }.toDS()
      (spark, wfConnectActTmp)
    } else {
      val sparkSession = SparkSession.builder().enableHiveSupport().appName(s"WIFI Recognize Graph App $datePartition ").getOrCreate()
      import sparkSession.implicits._
      sparkSession.sqlContext.setConf("hive.exec.dynamic.partition", "true")
      sparkSession.sqlContext.setConf("hive.exec.dynamic.partition.mode", "nonstrict")
      sparkSession.sqlContext.setConf("spark.default.parallelism", "300")
      sparkSession.sqlContext.setConf("spark.sql.shuffle.partitions", "300")
      val wfConnectActTmp: Dataset[UidConnectWifi] = sparkSession.sql(
        s"""
      select uid, wifi_name, geohash, ip, day_num, bssid
      from $sourceHiveTable
      where date_p=$datePartition
      """.stripMargin).rdd.zipWithUniqueId().map{case(row, vid) =>
        UidConnectWifi(row.getLong(0), parseStringElement(row.getString(1)), parseStringElement(row.getString(2)),
          parseStringElement(row.getString(3)), row.getInt(4), parseStringElement(row.getString(5)), vid)
      }.toDS()

      (sparkSession, wfConnectActTmp)
    }


    import spark.implicits._
    wfConnectActAll.persist(StorageLevel.MEMORY_AND_DISK)
    val wfConnectAct = wfConnectActAll.filter("bssid is null")
    wfConnectAct.createOrReplaceTempView(WIFI_CONNECT_ACTION)

    val uniqeWifi: Dataset[WifiNode] = spark.sql(
      s"""
        select min(vid), wifiName, ip
        from $WIFI_CONNECT_ACTION
         group by wifiName, ip
      """.stripMargin).map(row => WifiNode(row.getLong(0), row.getString(1), row.getString(2)))

    // 1. make vertex
    val wifiNodeRdd: Dataset[(VertexId, (String, String))] = uniqeWifi.map{ case WifiNode(vertexId, wifi, ip) => (vertexId, (wifi, ip))}

    // 2. make edge
    import spark.implicits._
    uniqeWifi.createOrReplaceTempView("wifi_node")
    val uid_wifi_connect: String = "uid_wifi_connect"
    spark.sql(
      s"""
        select vertexId, c.uid, w.wifiName, w.ip
        from wifi_node w inner join $WIFI_CONNECT_ACTION c on w.wifiName=c.wifiName and w.ip=c.ip
      """.stripMargin).createOrReplaceTempView(uid_wifi_connect)

    val uidConnitwifiNode: DataFrame = spark.sql(
      s"""
        select uid, wifiName, collect_set(vertexId)
        from $uid_wifi_connect
        group by uid, wifiName
      """.stripMargin
    )
    val relationships: Dataset[Edge[String]] = uidConnitwifiNode.filter(row => row.getSeq(2).length > 1).flatMap{row =>
      val vertexIdSeq: Seq[Long] = row.getSeq[Long](2)
      val relation_buffer: ArrayBuffer[Edge[String]] = new ArrayBuffer[Edge[String]]()

      for (i <- vertexIdSeq.indices) {
        for (j <- i + 1 until vertexIdSeq.size) {
          relation_buffer += Edge(vertexIdSeq(i), vertexIdSeq(j))
        }
      }
      relation_buffer
    }
    val graph = Graph(wifiNodeRdd.rdd, relationships.rdd)
    val connectedG: Graph[VertexId, String] = graph.connectedComponents(maxIter)
    val clusteredWifi: DataFrame  = graph.outerJoinVertices(connectedG.vertices) { case(vetexId, (vd, cc), op) =>
      (op, vd, cc)
    }.vertices.values.toDS().map{case(vetexId, wifiName, ip) => WifiNode(vetexId.get, wifiName, ip)}.toDF()
    clusteredWifi.createOrReplaceTempView("clustered_wifi")

    val clustedWifiAct: DataFrame = spark.sql(
      s"""
        select uid, a.wifiName, a.geohashOp, a.ip, bssid, a.dayNum, nvl(w.vertexId, a.vid) as wifi_id, $datePartition as date_p
        from $WIFI_CONNECT_ACTION a left join clustered_wifi w on a.wifiName=w.wifiName and a.ip=w.ip
      """.stripMargin)

    import org.apache.spark.sql.functions.{col, lit, when}
    val outputWifiAct: DataFrame = wfConnectActAll.filter("bssid is not null")
      //.withColumn("wifi_id", col("bssid"))
      .withColumn("date_p", lit(datePartition))
      .select($"uid", $"wifiName", $"geohashOp".alias("geohash"), $"ip", $"bssid", $"dayNum", $"bssid".alias("wifi_id"), $"date_p") union clustedWifiAct
    /* union
      clustedWifiAct.select($"uid", $"wifiName", $"geohashOp".alias("geohash"), $"ip", $"bssid", $"dayNum", $"wifi_id", $"date_p")*/
    if (isDebug) {
      outputWifiAct.show(20, false)
    } else {
      outputWifiAct.write.mode(SaveMode.Overwrite).insertInto(targetHiveTable)
    }
  }

  private def parseStringElement(param: String): String = {
    if (param == null || param.equalsIgnoreCase("null") || param.equalsIgnoreCase("\\N")) {
      null
    } else {
      param
    }
  }
}

case class WifiNode(vertexId: VertexId, wifiName: String, ip: String)
case class UidConnectWifi(uid: Long, wifiName: String, geohashOp: String, ip: String, dayNum: Int, bssid: String, vid: Long = 0)
