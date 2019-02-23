package com.robert.lab.reduce

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import scala.io.Source

object ReduceIDSetApp {

  val idCounter: AtomicInteger = new AtomicInteger(1)

  def main(args: Array[String]): Unit = {
    val filename = "/Users/yangqingfeng/workspace/yqf-workspace/demo-code/scala/scala-demo/data/idmap.dat"
    val rawIdPair = Source.fromFile(filename).getLines().map(_.split("\\s")).map{parts => (parts(0), parts(1))}.toList
    // uid -> Set(f1, f2, ...f*)
    val uidGrouped: Map[String, Set[String]] = rawIdPair.groupBy(_._1).mapValues{ listIdPair: List[(String, String)] =>
      listIdPair.map(uidWf => uidWf._2).toArray.toSet
    }
    val idMapRdd: List[IdMapSet] = uidGrouped.toList.sortBy(_._1).map{case(uid: String, wifiSet: Set[String]) =>
      val setId = createSetId()
      val uidSet = Set(uid)
      IdMapSet(setId, uidSet, wifiSet)
    }

    println(idMapRdd.mkString("\n"))
    println("========")
    val (notRelated, related) = reduceIDMapSet(idMapRdd)
    val (notR2, related2) = reduceIDMapSet(related.get)
    val (notR3, related3) = reduceIDMapSet(related2.get)
    println("------------ 2www--")
    reduceIDMapSet(related3.get)
  }

  private def mergeIdMapSetByKey(flattenDataset: List[(String, IdMapSet)]): Map[String, IdMapSet] = {
    val groupedDataSet: Map[String, IdMapSet] = flattenDataset.groupBy(_._1).mapValues{ idMapSet_list: List[(String, IdMapSet)] =>
      val list_IdMapSet: List[IdMapSet] = idMapSet_list.map(_._2)
      list_IdMapSet.foldLeft(IdMapSet(Integer.MAX_VALUE, Set[String](), Set[String](), Option(Set[Int]())))((accIdMapSet, nextIdMapSet) => {
        accIdMapSet + nextIdMapSet
      })
    }
    groupedDataSet
  }

  def reduceIDMapSet(idMapRdd: List[IdMapSet]): (Option[List[IdMapSet]], Option[List[IdMapSet]]) = {
    // 1. reduce by the wifi
    val flatten_wf_rdd: List[(String, IdMapSet)] = idMapRdd.flatten { uid_wif_set: IdMapSet =>
      uid_wif_set.wifiSet.map(wf_k => wf_k -> uid_wif_set)
    }
    println("1.1 Flatten Wifi: \n" + flatten_wf_rdd.mkString("\n"))
    val groupedWf: Map[String, IdMapSet] = mergeIdMapSetByKey(flatten_wf_rdd)
    printf("1.2 Grouped by WiFi, size %s: \n", groupedWf.size)
    printf(groupedWf.mkString("\n"))

    // 2. reduce by uid
    val flatten_uid_rdd: List[(String, IdMapSet)] = groupedWf.values.flatten { uid_wf_map: IdMapSet =>
      uid_wf_map.uidSet.map(uid_k => uid_k -> uid_wf_map)
    }.toList
    println("2.1 Flatten uid: \n" + flatten_uid_rdd.mkString("\n"))
    val groupedUid: Map[String, IdMapSet] = mergeIdMapSetByKey(flatten_uid_rdd)
    printf("1.2 Grouped by uid, size %s: \n", groupedUid.size)
    printf(groupedUid.toList.sortBy(_._1).mkString("\n"))

    val fullJoined_idMapSet: List[IdMapSet] = groupedUid.values.toList
    // 3. data set not relative to other set
    val idMapSet_notRelated: List[IdMapSet] = fullJoined_idMapSet.filterNot(_.flag)
    println("\nNoRelated: " + idMapSet_notRelated.mkString("\n"))
    val flatten_setId_rdd: List[(String, IdMapSet)] = fullJoined_idMapSet.filter(_.flag).map(idMapSet => idMapSet.setId.toString -> idMapSet)
    val reducedIdMapSet_list: List[(String, IdMapSet)] = mergeIdMapSetByKey(flatten_setId_rdd).toList
    printf("3.2 Grouped by setId, size %s: \n", reducedIdMapSet_list.size)
    printf(reducedIdMapSet_list.sortBy(_._1).mkString("\n"))

    (Some(idMapSet_notRelated.map(_.resetFlag)), Some(reducedIdMapSet_list.map(_._2).map(_.resetFlag)))
  }


  def createSetId(): Int = {
    return idCounter.getAndIncrement()
  }
}

case class IdMapSet(setId: Int, uidSet: Set[String], wifiSet: Set[String], mergedSet: Option[Set[Int]] = None, flag: Boolean = false) {
  def + (otherIdMapSet: IdMapSet): IdMapSet = {
    val new_setId = Math.min(setId, otherIdMapSet.setId)
    val new_uidSet = uidSet ++: otherIdMapSet.uidSet
    val new_wfSet = wifiSet ++: otherIdMapSet.wifiSet
    val new_mergedSet: Option[Set[Int]] = mergedSet match {
      case Some(setIdSet: Set[Int]) => {
        val tmpSetId = (setIdSet + setId + otherIdMapSet.setId).filter(_ < Integer.MAX_VALUE)
        Option(tmpSetId ++: otherIdMapSet.mergedSet.getOrElse(Set[Int]()))
      }
      case None => otherIdMapSet.mergedSet
    }

    val merged = new_uidSet.size > Math.max(uidSet.size, otherIdMapSet.uidSet.size) || new_wfSet.size > Math.max(wifiSet.size, otherIdMapSet.wifiSet.size)
    val new_flag = flag || otherIdMapSet.flag || merged
    IdMapSet(new_setId, new_uidSet, new_wfSet, new_mergedSet, new_flag)
  }

  def resetFlag = {
    IdMapSet(setId, uidSet, wifiSet, mergedSet)
  }
}
case class IdMap(uid: String, wifi: String)