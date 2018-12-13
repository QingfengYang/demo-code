package com.lab.spark.applist

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession


class JsonFilePathGenerator(val pathPattern: String, val date: String, val spark: SparkSession) {

  def getJsonFilePaths(): Seq[String] = {
    val inputPathSeq: Seq[String] = genJsonFilePaths(pathPattern, date)
    val targetPathSeq: Seq[String] = filterHadoopPathExists(inputPathSeq, spark)
    targetPathSeq
  }

  /*
    app_path_pattern = /data/applist/app_key_p={APP_KEY}/type_p={FILE_TYPE}/{FILE_TYPE}_{DATE}.txt
   */
  def genJsonFilePaths(pathPattern: String, date: String): Seq[String] = {
    val pathSeq: Seq[String] = JsonFilePathGenerator.APP_KEYS_SEQ.flatMap{appkey: String =>
      JsonFilePathGenerator.INSTALL_APP_TYPES.map(ftype => pathPattern.replaceAll("\\{APP_KEY}", appkey)
        .replaceAll("\\{FILE_TYPE}", ftype).replaceAll("\\{DATE}", date))
    }
    pathSeq
  }

/*
  def filterLocalPathExists(origPaths: Seq[String]): Seq[String] = {

  }
*/

  def filterHadoopPathExists(origPaths: Seq[String], spark: SparkSession): Seq[String] = {
    val hadoopfs: FileSystem = FileSystem.get(spark.sparkContext.hadoopConfiguration)

    def checkDirExist(path: String): Boolean = {
      val p = new Path(path)
      hadoopfs.exists(p) && hadoopfs.getFileStatus(p).isDirectory
    }

    val filteredPaths = origPaths.filter(p => checkDirExist(p))

    //println(s"orig paths: $origPaths")
    //println(s"filteredPaths paths: $filteredPaths")

    filteredPaths
  }

}

object JsonFilePathGenerator{
  val APP_KEYS_SEQ = Seq("C671B74C5E41CFC9", "F9CC8787275D8691", "6BE944F6D5BE79B7", "2214D380CFED6A21", "C4FAF9CE1569F541")
  //val APP_KEYS_SEQ = Seq("C671B74C5E41CFC9")
  val INSTALL_APP_TYPES = Seq("f1", "f2", "f3")
}