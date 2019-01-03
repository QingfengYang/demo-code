package com.lab.spark.applist

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession


class JsonFilePathGenerator(val pathPattern: String, val dateSeq: Seq[String], val spark: SparkSession) {

  /*
      app_path_pattern = /data/applist/app_key_p={APP_KEY}/type_p={FILE_TYPE}/{FILE_TYPE}_{DATE}.txt
   */
  def getInstalledAppJsonFilePaths: Seq[String] = {
    val inputPathSeq: Seq[String] = getAppJsonFilePaths(JsonFilePathGenerator.INSTALL_APP_TYPES)
    val targetPathSeq: Seq[String] = filterHadoopPathExists(inputPathSeq, spark)
    targetPathSeq
  }

  def getOpenedAppJsonFilePaths: Seq[String] = {
    val inputPathSeq: Seq[String] = getAppJsonFilePaths(JsonFilePathGenerator.OPEN_APP_TYPES)
    val targetPathSeq: Seq[String] = filterHadoopPathExists(inputPathSeq, spark)
    targetPathSeq
  }

  def getAppJsonFilePaths(appFileTypes: Seq[String]): Seq[String] = {
    val inputPathSeq: Seq[String] = dateSeq.flatMap{ date =>
      JsonFilePathGenerator.APP_KEYS_SEQ.flatMap{appkey: String =>
        appFileTypes.map(ftype => pathPattern.replaceAll("\\{APP_KEY}", appkey)
          .replaceAll("\\{FILE_TYPE}", ftype).replaceAll("\\{DATE}", date))
      }
    }
    val targetPathSeq: Seq[String] = filterHadoopPathExists(inputPathSeq, spark)
    targetPathSeq
  }


  private[this] def filterHadoopPathExists(origPaths: Seq[String], spark: SparkSession): Seq[String] = {
    val hadoopfs: FileSystem = FileSystem.get(spark.sparkContext.hadoopConfiguration)

    def checkDirExist(path: String): Boolean = {
      val p = new Path(path)
      hadoopfs.exists(p) && hadoopfs.getFileStatus(p).isDirectory
    }

    val filteredPaths = origPaths.filter(p => checkDirExist(p))
    filteredPaths
  }

}

object JsonFilePathGenerator{
  val APP_KEYS_SEQ = Seq("C671B74C5E41CFC9", "F9CC8787275D8691", "6BE944F6D5BE79B7", "2214D380CFED6A21", "C4FAF9CE1569F541")
  //val APP_KEYS_SEQ = Seq("C671B74C5E41CFC9")
  val INSTALL_APP_TYPES = Seq("f1", "f2", "f3")
  val OPEN_APP_TYPES = Seq("f4")
}

/*
object Test {
  def main(args: Array[String]): Unit = {
    val pathPattern = "/Users/yangqingfeng/tmp/debug/applist/date_p={DATE}/app_key_p={APP_KEY}/type_p={FILE_TYPE}"
    val dateSeq = Seq("20181211", "20181212", "20181213")
    val generator = new JsonFilePathGenerator(pathPattern, dateSeq, null)
    println(generator.getAppJsonFilePaths(JsonFilePathGenerator.INSTALL_APP_TYPES).mkString("\n"))
  }
}*/
