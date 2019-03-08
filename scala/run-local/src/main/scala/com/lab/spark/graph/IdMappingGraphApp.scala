package com.lab.spark.graph

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx.{Edge, Graph, GraphLoader, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import java.util.UUID

import org.apache.spark.graphx.lib.ConnectedComponents
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable.ArrayBuffer

object IdMappingGraphApp {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().master("local[2]").appName("Id Mapping Graph Application").getOrCreate()
    import spark.implicits._

    val wfConnectAct: Dataset[UidConnectWifi] = spark.sparkContext.textFile("data/graphx/wf_connection.txt").map(line => line.split("\\s"))
      .map(parts => UidConnectWifi(parts.head.toLong, parts(1), parseStringElement(parts(2)), parts(3), parts(4).toInt)).toDS()
    wfConnectAct.createOrReplaceTempView("wifi_connect_action")
    wfConnectAct.persist(StorageLevel.MEMORY_AND_DISK)
/*
    wfConnectAct.map{case UidConnectWifi(uid, wifi, geohashOp, ip, dayNum) =>
      val wifiWithIp = s"$wifi-$ip"
      (wifiWithIp, (uid, wifi, ip))
    }*/

    // 1. make vertex
    val uniqeWifi: Dataset[WifiNode] = wfConnectAct.map{case UidConnectWifi(uid, wifi, geohashOp, ip, dayNum) => (wifi, ip)}.distinct()
      .map{case(wifi, ip) =>
      val wifiIp = wifi + ip
      val vertiexId: Long = UUID.nameUUIDFromBytes(wifiIp.getBytes).getMostSignificantBits
        WifiNode(vertiexId, wifi, ip)
    }

    val wifiNodeRdd: Dataset[(VertexId, (String, String))] = uniqeWifi.map{ case WifiNode(vertexId, wifi, ip) => (vertexId, (wifi, ip))}

    // 2. make edge
    //val wifiRelatedRdd:
    import spark.implicits._
    uniqeWifi.createOrReplaceTempView("wifi_node")
    wfConnectAct.createOrReplaceTempView("wifi_connect_action")
    spark.sql(
      """
        select vertexId, c.uid, w.wifiName, w.ip
        from wifi_node w left join wifi_connect_action c on w.wifiName=c.wifiName and w.ip=c.ip
      """.stripMargin).createOrReplaceTempView("uid_wifi_connect")

    val uidConnitwifiNode: DataFrame = spark.sql(
      """
        select uid, wifiName, collect_set(vertexId)
        from uid_wifi_connect
        group by uid, wifiName
      """.stripMargin
    )
    //uidConnitwifiNode.flatMap(row => row.getSeq[Long](2)).show(10)
    val relationships: Dataset[Edge[String]] = uidConnitwifiNode.filter(row => row.getSeq(2).length > 1).flatMap{row =>
      val vertexIdSeq: Seq[Long] = row.getSeq[Long](2)
      val relation_buffer: ArrayBuffer[Edge[String]] = new ArrayBuffer[Edge[String]]()

      for (i <- vertexIdSeq.indices) {
        for (j <- i + 1 until vertexIdSeq.size) {
          //relation_buffer += vertexIdSeq(i) + "_" + vertexIdSeq(j)
          //relation_buffer += s"uid=${row.getLong(0)}-${row.getString(1)}=${vertexIdSeq(i)}-${vertexIdSeq(j)}"
          relation_buffer += Edge(vertexIdSeq(i), vertexIdSeq(j), s"${row.getLong(0)}->${row.getString(1)}")
        }
      }
      relation_buffer
    }
    val graph = Graph(wifiNodeRdd.rdd, relationships.rdd)
    //graph.connectedComponents().vertices.map{case(vid_1: VertexId, vid_2: VertexId) => vid_2 -> vid_1}
    val connectedG = graph.connectedComponents()
    val clusteredWifi: DataFrame  = graph.outerJoinVertices(connectedG.vertices) { case(vetexId, (vd, cc), op) =>
      (op, vd, cc)
    }.vertices.values.toDS().map{case(vetexId, wifiName, ip) => WifiNode(vetexId.get, wifiName, ip)}.toDF()
    clusteredWifi.createOrReplaceTempView("clustered_wifi")

    spark.sql(
      """
        select vertexId, uid, a.wifiName, a.ip, a.geohashOp, a.dayNum
        from wifi_connect_action a left join clustered_wifi w on a.wifiName=w.wifiName and a.ip=w.ip
        order by vertexId, uid, wifiName, ip
      """.stripMargin).show(10, false)
    /*val joined = wifiDS.join(wifiConnectDS, Seq[String]("wifiName", "ip"), "left_outer")
    val samples = joined.take(10)
    samples.foreach(println(_))*/

  }

  private def parseStringElement(param: String): Option[String] = {
    if (param == null || param.equalsIgnoreCase("null") || param.equalsIgnoreCase("\\N")) {
      None
    } else {
      Some(param)
    }
  }
}

case class WifiNode(vertexId: VertexId, wifiName: String, ip: String)
case class UidConnectWifi(uid: Long, wifiName: String, geohashOp: Option[String], ip: String, dayNum: Int)
