package com.lab.spark

import com.lab.spark.applist.DateUtil
import org.scalatest.FlatSpec

class TestDateUtil extends FlatSpec{

  "generate data seqs" should "parse" in {
    val dateStr: String = "20181210"
    val dateRange: Int = 30
    DateUtil.genDateSeq(dateStr, dateRange).foreach(println _)
  }
}
