package com.lab.spark

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx.{EdgeContext, Graph, VertexRDD}
import org.apache.spark.graphx.util.GraphGenerators

object GraphAggMessageApp {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local").setAppName("Graph Agg message App")
    val sc = new SparkContext(conf)
    val graph: Graph[Double, Int] = GraphGenerators.logNormalGraph(sc, numVertices = 5).mapVertices((id, _) => id.toDouble)
    graph.cache()
    graph.vertices.collect().foreach(println(_))
    graph.edges.collect().foreach(println(_))
    val olderFollowers: VertexRDD[(Int, Double)] = graph.aggregateMessages[(Int, Double)](
      triplet => {
        if (triplet.srcAttr > triplet.dstAttr) {
          triplet.sendToDst((1, triplet.srcAttr))
        }
      },
      (a, b) => (a._1 + b._1, a._2 + b._2)
    )

    val avgAgeOfOlderFollowers: VertexRDD[Double] = olderFollowers.mapValues(
      (id, value) => value match { case(count, totalAge) => totalAge / count}
    )

    avgAgeOfOlderFollowers.collect.foreach(println(_))
  }
}
