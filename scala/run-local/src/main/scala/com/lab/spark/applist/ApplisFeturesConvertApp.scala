package com.lab.spark.applist

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable.ArrayBuffer

object ApplisFeturesConvertApp {

  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      val usage =
        """
          Usage: input_path_pattern, date, output_path
        """.stripMargin
    }
    // 20181120
    val date = args(0)
    //  /Users/yangqingfeng/tmp/debug/applist/date_p={DATE}/app_key_p={APP_KEY}/type_p={FILE_TYPE}/{FILE_TYPE}_{DATE}.txt
    val pathPattern = args(1)
    val appMetaPath = args(2)

    val sparkSession = SparkSession.builder().master("local").appName("Analysis app list").getOrCreate()

    //val guidTagDF: Dataset[(String, String)] = applistDF.flatMap(row => row.getSeq[String](1).map((_, row.getString(0))))
    import sparkSession.implicits._
    val applistDF: DataFrame = loadInstallAppDataFrame(date, pathPattern, sparkSession)
    applistDF.persist(StorageLevel.MEMORY_AND_DISK_SER)

    val guidTagAppDF: Dataset[GuidInstalledPkg] = applistDF.flatMap{row =>
      val guid: String = row.getString(0).trim
      row.getSeq[String](1).map(pkg => GuidInstalledPkg(guid, pkg.trim))
    }
    guidTagAppDF.createOrReplaceTempView("gid_pkg_table")

    val appPKgMetaDF: Dataset[PkgMeta] = loadAppPkgMetaDataFrame(appMetaPath, sparkSession)
    appPKgMetaDF.createOrReplaceTempView("dm_swa_app_tag")

    val joinSQL =
      """
        select a.guid, a.appPkg, m.category, m.subCategory, m.tags
        from gid_pkg_table a left join dm_swa_app_tag m
        on a.appPkg=m.pkg
      """.stripMargin
    val userAppMetaDF: DataFrame = sparkSession.sql(joinSQL)
    val userAppCategoryKeyDF: Dataset[String] = userAppMetaDF.flatMap(row => extractUserCategoryNTags(row))
    //userAppCategoryKeyDF.show(5, false)

    //(guid|category|category_value, num) / (guid|tags|tag, num)
    //val categoryOccursDF: DataFrame = userAppCategoryKeyDF.map(key => (key, 1)).groupBy($"_1").sum("_2")

    // (guid|category|category_value, num) / (guid|tags|tag, num)
    val userAppCategoryOccursDF: RDD[(String, Int)] = userAppCategoryKeyDF.rdd.map(key => (key, 1)).reduceByKey(_ + _)
    //userAppCategoryOccursDF.take(5)
    // 923296ca30d7a982cc7b178c3db318ec,tags|社区:1,subCategory|交友:1,category|通讯社交:3
    val userCategoryOccursStrRdd: RDD[(String, String)] = userAppCategoryOccursDF.map{ case(guidCategoryKey, count) =>
      val firstSepIndex: Int = guidCategoryKey.indexOf("|")
      val guid: String = guidCategoryKey.substring(0, firstSepIndex)
      val categoryOccur: String = guidCategoryKey.substring(firstSepIndex + 1)
      guid -> s"$categoryOccur|$count"
    }.reduceByKey{ case(str1, str2) =>
      s"$str1,$str2"
    }

    case class CategoryOccurs(categoryType: String, categoryKey: String, occurs: Int)
    val userCategoryOccurRdd: RDD[(String, Array[CategoryOccurs])] = userCategoryOccursStrRdd.mapValues{ categoryOccursStrSeq: String =>
      categoryOccursStrSeq.split(",").map{ occursStr =>
        val parts: Array[String] = occursStr.split("\\|")
        try {
          parts(2).toInt
        } catch {
          case e: Exception => println(s"occursStr=$occursStr, e=$e")
        }
        CategoryOccurs(parts(0), parts(1), parts(2).toInt)
      }
    }

    //import sparkSession.sqlContext.implicits._
    import sparkSession.implicits._

    val userCategoryUsageDF: Dataset[UserAppUsage]= userCategoryOccurRdd.map { case (guid: String, categoryOccurArr: Array[CategoryOccurs]) =>
      val categoryUsageArr: Array[CategoryUsage] = categoryOccurArr.filter(categoryOccur => categoryOccur.categoryType.equals("category")).map(categoryOccur => CategoryUsage(categoryOccur.categoryKey, categoryOccur.occurs))
      val subCategoryUsageArr: Array[CategoryUsage] = categoryOccurArr.filter(categoryOccur => categoryOccur.categoryType.equals("subCategory")).map(categoryOccur => CategoryUsage(categoryOccur.categoryKey, categoryOccur.occurs))
      val tagUsageArr: Array[CategoryUsage] = categoryOccurArr.filter(categoryOccur => categoryOccur.categoryType.equals("tags")).map(categoryOccur => CategoryUsage(categoryOccur.categoryKey, categoryOccur.occurs))
      UserAppUsage(guid, categoryUsageArr, subCategoryUsageArr, tagUsageArr)
    }.toDS
    userCategoryUsageDF.sample(true, 0.2).write.format("parquet").save("/Users/yangqingfeng/tmp/debug/applist/app_usage/")
    //println(userCategoryOccursStrRdd.take(5).mkString("\n"))
    //categoryOccursDF.rdd.map(row => row.getString(0))

    sparkSession.stop()
  }

  /*
   * Row(guid, appPkg, pkg, category, subCategory, tags)
   */
  def extractUserCategoryNTags(row: Row): Seq[String] = {
    val arrBuf: ArrayBuffer[String] = new ArrayBuffer[String]()

    if (row.getString(2) != null) {
      val userCategory: String = s"${row.getString(0)}|category|${row.getString(2)}"
      arrBuf += userCategory
    }

    if (row.getString(3) != null) {
      val userSugCategory: String = s"${row.getString(0)}|subCategory|${row.getString(3)}"
      arrBuf += userSugCategory
    }

    val tags: String = row.getString(4)
    if (tags != null && tags.length > 0) {
      tags.split(",").foreach{ tag =>
        val userTag: String = s"${row.getString(0)}|tags|$tag"
        arrBuf += userTag
      }
    }
    arrBuf
  }

  def loadAppPkgMetaDataFrame(path: String, sparkSession: SparkSession): Dataset[PkgMeta] = {
    import sparkSession.implicits._
    val appMetaDF: DataFrame = sparkSession.read.option("delimiter", "\t").format("csv").csv(path)
    appMetaDF.flatMap{row =>
      Seq[PkgMeta](PkgMeta(row.getString(2), row.getString(5), row.getString(7), row.getString(10)),
        PkgMeta(row.getString(3), row.getString(5), row.getString(7), row.getString(10))
      )
    }
  }

  def loadInstallAppDataFrame(date: String, pathPattern: String, sparkSession: SparkSession): DataFrame = {
    val json_exists_paths = new JsonFilePathGenerator(pathPattern, date, sparkSession).getJsonFilePaths()

    val jsonDF = sparkSession.read.format("json").json(json_exists_paths:_*)
    val applistDF = jsonDF.select("guid", "installed_applist").filter(row => !row.isNullAt(0))

    //val guidTagDF: Dataset[(String, String)] = applistDF.flatMap(row => row.getSeq[String](1).map((_, row.getString(0))))
    /*val guidTagAppDF: Dataset[GuidInstalledPkg] = applistDF.flatMap{row =>
      val guid: String = row.getString(0).trim
      row.getSeq[String](1).map(pkg => GuidInstalledPkg(guid, pkg.trim))
    }*/

    applistDF
  }
}

case class GuidInstalledPkg(guid: String, appPkg: String)
case class PkgMeta(pkg: String, category: String, subCategory: String, tags: String)

case class CategoryUsage(category: String, occurs: Int)
case class UserAppUsage(guid: String, categoryUsage: Array[CategoryUsage], subCategoryUsage: Array[CategoryUsage], tagUsage: Array[CategoryUsage])
