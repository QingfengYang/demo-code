package com.robert.lab

import com.robert.lab.reduce.IdMap

object UtilTester {

  def main(args: Array[String]): Unit = {
    val list = List(("abc",1),("abc",2),("cbe",5),("cab",1))
    val ret = list.groupBy(_._1).mapValues(_.map(_._2))
    print(ret)

    val idMapList = List(
      IdMap("u1", "f1"),
      IdMap("u1", "f1"),
      IdMap("u1", "f1")
    )
    val ret1 = idMapList.groupBy(_.uid)
    print(ret1)
  }
}
