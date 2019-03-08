package com.lab.spark.graph

import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object SocialNetDemoApp {

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf().setMaster("local[2]").setAppName("Social Network Application1"))
    // create an RDD for vertices
    val users: RDD[(VertexId, (String, String))] =
      sc.parallelize(Array(
        (3L, ("rxin", "student")),
        (7L, ("jgonzal", "postdoc")),
        (5L, ("franklin", "prof")),
        (2L, ("istoica", "prof")),
        (100L, ("istoica", "prof")),
        (101L, ("istoica", "prof"))
      ))

    // create an RDD for edges
    val relationships: RDD[Edge[String]] =
      sc.parallelize(Array(
        Edge(3L, 7L, "collab"),
        Edge(5L, 3L, "advisor"),
        Edge(2L, 5L, "colleague"),
        Edge(5L, 7L, "pi"),
        Edge(100L, 101L, "canada")))

    // Define a default user in case there are relationship with missing user
    val defaultUser = ("John Doe", "Missiong")
    val graph = Graph(users, relationships, defaultUser)

    /*val facts: RDD[String] = graph.triplets.map(triplet =>
      triplet.srcAttr._1 + " is the " + triplet.attr + " of " + triplet.dstAttr._1)

    facts.collect().foreach(println(_))*/
    graph.connectedComponents().vertices.map{case(vid_1: VertexId, vid_2: VertexId) => vid_2 -> vid_1.toString}.reduceByKey((v1, v2) => v1 + "," + v2).values.collect().foreach(println(_))

  }
}
