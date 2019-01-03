package com.lab.spark.applist

import java.text.SimpleDateFormat
import java.util.Calendar

object DateUtil {
  val simpleDateFormat = new SimpleDateFormat("yyyyMMdd")
  def genDateSeq(dateStr: String, daysRange: Int): Seq[String] = {
    val date = simpleDateFormat.parse(dateStr)
    val calendar = Calendar.getInstance()
    calendar.setTime(date)

    val dateSeq: Seq[String] = for (i <- 0 to daysRange) yield {
      val time = if (i > 0) {
        calendar.add(Calendar.DATE, -1)
        calendar.getTime
      } else {
        calendar.getTime
      }
      simpleDateFormat.format(time)
    }
    dateSeq
  }
}
