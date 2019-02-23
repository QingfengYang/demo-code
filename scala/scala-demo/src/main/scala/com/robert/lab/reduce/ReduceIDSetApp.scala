package com.robert.lab.reduce

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import scala.io.Source

object ReduceIDSetApp {

  val idCounter: AtomicInteger = new AtomicInteger()

  def main(args: Array[String]): Unit = {
    val filename = "/Users/yangqingfeng/workspace/yqf-workspace/demo-code/scala/scala-demo/data/idmap.dat"
    val rawIdPair = Source.fromFile(filename).getLines().map(_.split("\\s")).map{parts => (parts(0), parts(1))}.toList
    // uid -> Set(f1, f2, ...f*)
    val uidGrouped: Map[String, Set[String]] = rawIdPair.groupBy(_._1).mapValues{ listIdPair: List[(String, String)] =>
      listIdPair.map(uidWf => uidWf._2).toArray.toSet
    }
    val idMapRdd: List[IdMapSet] = uidGrouped.map{case(uid: String, wifiSet: Set[String]) =>
      val setId = createSetId()
      val uidSet = Set(uid)
      IdMapSet(setId, uidSet, wifiSet)
    }.toList


    println(idMapRdd.mkString("\n"))
    println("========")
    reduceIDMapSet(idMapRdd)
  }

  def reduceIDMapSet(idMapRdd: List[IdMapSet]): (Option[List[IdMapSet]], Option[List[IdMapSet]]) = {
    // 1. reduce by the second key
    val flatten_wf_rdd: List[(String, IdMapSet)] = idMapRdd.flatten { uid_wif_set: IdMapSet =>
      uid_wif_set.wifiSet.map(wf_k => wf_k -> uid_wif_set)
    }
    println("Flatten Wifi: \n" + flatten_wf_rdd.mkString("\n"))
    val groupedWf: Map[String, IdMapSet] = flatten_wf_rdd.groupBy(_._1).mapValues{ idMapSet_list: List[(String, IdMapSet)] =>
      val list_IdMapSet: List[IdMapSet] = idMapSet_list.map(_._2)
      list_IdMapSet.foldLeft(IdMapSet(-1, Set[String](), Set[String](), Option(Set[Int]())))((accIdMapSet, nextIdMapSet) => {
        accIdMapSet + nextIdMapSet
      })
    }
    printf("1. Grouped by WiFi, size %s: \n", groupedWf.size)
    printf(groupedWf.mkString("\n"))

    val flatten_uid_rdd: List[(String, IdMapSet)] = groupedWf.values.flatten { uid_wf_map: IdMapSet =>
      uid_wf_map.uidSet.map(uid_k => uid_k -> uid_wf_map)
    }.toList
    println("Flatten Wifi: \n" + flatten_uid_rdd.mkString("\n"))
    flatten_uid_rdd.groupBy(_._1).mapValues{idMapSet_list: List[(String, IdMapSet)] =>
      val list_IdMapSet: List[IdMapSet] = idMapSet_list.map(_._2)
      list_IdMapSet.foldLeft(IdMapSet(-1, Set[String](), Set[String](), Option(Set[Int]())))((accIdMapSet, nextIdMapSet) => {
        accIdMapSet + nextIdMapSet
      })
    }
/*

    def mergeIdMapSetByKey(flatten_wf_rdd: List[(String, IdMapSet)]): Map[String, IdMapSet] = {
      println("Flatten Wifi: \n" + flatten_wf_rdd.mkString("\n"))
      val groupedWf: Map[String, IdMapSet] = flatten_wf_rdd.groupBy(_._1).mapValues{ idMapSet_list: List[(String, IdMapSet)] =>
        val list_IdMapSet: List[IdMapSet] = idMapSet_list.map(_._2)
        list_IdMapSet.foldLeft(IdMapSet(-1, Set[String](), Set[String](), Option(Set[Int]())))((accIdMapSet, nextIdMapSet) => {
          accIdMapSet + nextIdMapSet
        })
      }
    }
*/

    (None, None)
  }


  def createSetId(): Int = {
    return idCounter.getAndIncrement()
  }

  def createEmpth(): Unit = {
    IdMapSet(-1, Set[String](), Set[String](), Option(Set[Int]()))
  }
}

case class IdMapSet(setId: Int, uidSet: Set[String], wifiSet: Set[String], mergedSet: Option[Set[Int]] = None, flag: Boolean = false) {
  def + (otherIdMapSet: IdMapSet): IdMapSet = {
    val new_setId = Math.max(setId, otherIdMapSet.setId)
    val new_uidSet = uidSet ++: otherIdMapSet.uidSet
    val new_wfSet = wifiSet ++: otherIdMapSet.wifiSet
    val new_mergedSet: Option[Set[Int]] = mergedSet match {
      case Some(setIdSet: Set[Int]) => {
        val tmpSetId = (setIdSet + setId + otherIdMapSet.setId).filter(_ > -1)
        Option(tmpSetId ++: otherIdMapSet.mergedSet.getOrElse(Set[Int]()))
      }
      case None => otherIdMapSet.mergedSet
    }
    val new_flag = flag && otherIdMapSet.flag
    IdMapSet(new_setId, new_uidSet, new_wfSet, new_mergedSet, new_flag)
  }
}
case class IdMap(uid: String, wifi: String)