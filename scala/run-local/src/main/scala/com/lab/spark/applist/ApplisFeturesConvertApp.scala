package com.lab.spark.applist

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable.ArrayBuffer

object ApplisFeturesConvertApp {

  val DM_APP_CATEGORY_TAGS: String = "dm_app_category_tags"
  // todo
  val dm_swa_app_tag: String = "dm_swa_app_tag"

  val dm_category_tbl: String = "dm_app_category"
  val dm_tag_tbl: String = "dm_app_tag"

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

    val sparkSession = SparkSession.builder().master("local[2]").appName("Analysis app list").getOrCreate()

    // 1. load app metadata
    initAppMetaTable(appMetaPath, sparkSession)
   /* val appPKgMetaDF: Dataset[AppMeta] = loadAppPkgMetaDataFrame(appMetaPath, sparkSession)
    appPKgMetaDF.createOrReplaceTempView(s"$dm_swa_app_tag")
    appPKgMetaDF.persist(StorageLevel.MEMORY_AND_DISK)
    // 2. calclulate appInstalled
    //val joinUseageDF: Dataset[Row] = calculateInstalledAppDF(sparkSession, date, pathPattern)
*/
    calculateOpenAppDF(sparkSession, date, pathPattern)

    sparkSession.stop()
  }


  def calculateOpenAppDF(spark: SparkSession, date: String,
                         pathPattern: String): Unit = {
    val openAppDF = loadOpenAppDataFrame(date, pathPattern, spark)
    val app_open_tbl = "app_open_tbl"
    openAppDF.createOrReplaceTempView(s"$app_open_tbl")
    //openAppDF.show(3, false)

    val app_open_action_tbl = "app_open_action_tbl"
    spark.sql(s"select guid, app_name, from_unixtime(cast(opt_time.open_time as bigint)/1000, 'yyyyMMdd') optTime from $app_open_tbl LATERAL VIEW explode(open_time_and_ip) opt_a AS opt_time ") //@TODO debug
      .createOrReplaceTempView(s"$app_open_action_tbl")
/*

    /* 1. 安装app计算使用频次 */
    val user_app_open_freq_tbl = "user_app_open_freq_tbl"
    spark.sql(s"select guid, app_name, count(distinct optTime) as openDayCount, count(optTime) as openTimes from $app_open_action_tbl group by guid, app_name ")
      .createOrReplaceTempView(s"$user_app_open_freq_tbl")

    val userAppOpenTbl = "user_app_open_stat_tbl"
    spark.sql(s"select guid, struct(app_name, openDayCount) as appOpenDayCount, struct(app_name, openTimes) appOpenTimes from $user_app_open_freq_tbl")
        .createOrReplaceTempView(s"$userAppOpenTbl")
*/


    /* 2. 按app类别计算使用频次 */
    /* 2.1 category 统计 */
    spark.sql(
      s"""
          select guid, collect_list(category_openDayCount) as category_openDayCount, collect_list(category_openTimes) as category_openTimes
          from(
            select a.guid, struct(m.category, count(distinct a.optTime)) as category_openDayCount, struct(m.category, count(optTime)) as category_openTimes
              from $app_open_action_tbl a inner join $DM_APP_CATEGORY_TAGS m on a.app_name=m.app_name
              where category is not null and length(category) > 0
              group by guid, category
            ) group by guid
      """.stripMargin).createOrReplaceTempView("tmp_category_open_stat_final")

    spark.sql(
      s"""
          select guid, collect_list(subCategory_openDayCount) as subCategory_openDayCount, collect_list(subCategory_openTimes) as subCategory_openTimes
          from(
             select a.guid, struct(m.subCategory, count(distinct a.optTime)) as subCategory_openDayCount, struct(m.subCategory, count(optTime)) as subCategory_openTimes
               from $app_open_action_tbl a inner join $DM_APP_CATEGORY_TAGS m on a.app_name=m.app_name
                where subCategory is not null and length(subCategory) > 0
               group by guid, subCategory
           ) group by guid
      """.stripMargin).createOrReplaceTempView("tmp_subCategory_open_stat_final")

    /* 2.2 计算tags标签使用次数 */
    spark.sql(s"select app_name, split(tags, ',') as tag_array from $DM_APP_CATEGORY_TAGS where tags is not null and length(tags) > 0 ").createOrReplaceTempView("app_tags_meta")
    spark.sql("select app_name, tag from app_tags_meta LATERAL VIEW explode(tag_array) tag_tbl as tag ").createOrReplaceTempView("app_tag_mapping")
    /*spark.sql(s"select a.guid, a.app_name, a.optTime, m.tag from $app_open_action_tbl a left join app_tag_mapping m on a.app_name=m.app_name ").createOrReplaceTempView("tmp_tag_open_action")
    spark.sql("select guid, tag, count(distinct optTime) as openDayCount, count(optTime) as openTimes from tmp_tag_open_action group by guid, tag ").createOrReplaceTempView("tmp_tag_open_stat")
    spark.sql("select guid, struct(tag, openDayCount) as tag_open_day_count, struct(tag, openTimes) as tag_open_times from tmp_tag_open_stat").createOrReplaceTempView("tmp_tag_open_stat_final")
*/
    spark.sql(
      s"""
        select guid, collect_list(tag_open_day_count) as tag_open_day_count, collect_list(tag_open_times) as tag_open_times
        from (
          select guid, struct(tag, count(distinct optTime)) as tag_open_day_count, struct(tag, count(optTime)) as tag_open_times
          from $app_open_action_tbl a inner join app_tag_mapping m on a.app_name=m.app_name
          group by guid, tag
        ) group by guid
      """.stripMargin).createOrReplaceTempView("tmp_tag_open_stat_final")

    spark.sql(
      """
        select c.guid, category_openDayCount, category_openTimes, subCategory_openDayCount, subCategory_openTimes, tag_open_day_count, tag_open_times
        from tmp_category_open_stat_final c left join tmp_subCategory_open_stat_final s on c.guid=s.guid
              left join tmp_tag_open_stat_final t on c.guid=t.guid
      """.stripMargin).show(3, false)

   /* spark.sql(
      """
         select * from
         (
          select c.guid, category_openDayCount, category_openTimes, subCategory_openDayCount, subCategory_openTimes
          from tmp_category_open_stat_final c left join tmp_subCategory_open_stat_final s on c.guid=s.guid
          ) a left join ( select * from tmp_tag_open_stat_final ) t on a.guid=t.guid
      """.stripMargin).show(3, false)*/
  }


  def calculateInstalledAppDF(sparkSession: SparkSession, date: String,
                              pathPattern: String): DataFrame = {
    import sparkSession.implicits._
    val applistDF: DataFrame = loadInstallAppDataFrame(date, pathPattern, sparkSession)
    applistDF.persist(StorageLevel.MEMORY_AND_DISK_SER)

    val guidTagAppDF: Dataset[GuidInstalledApp] = applistDF.flatMap{ row =>
      val guid: String = row.getString(0).trim
      row.getSeq[String](1).map(pkg => GuidInstalledApp(guid, pkg.trim))
    }
    guidTagAppDF.createOrReplaceTempView("gid_pkg_table")


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

    val userCategoryUsageDF: Dataset[UserCategoryInstalled]= userCategoryOccurRdd.map { case (guid: String, categoryOccurArr: Array[CategoryOccurs]) =>
      val categoryUsageArr: Array[CategoryInstalled] = categoryOccurArr.filter(categoryOccur => categoryOccur.categoryType.equals("category")).map(categoryOccur => CategoryInstalled(categoryOccur.categoryKey, categoryOccur.occurs))
      val subCategoryUsageArr: Array[CategoryInstalled] = categoryOccurArr.filter(categoryOccur => categoryOccur.categoryType.equals("subCategory")).map(categoryOccur => CategoryInstalled(categoryOccur.categoryKey, categoryOccur.occurs))
      val tagUsageArr: Array[CategoryInstalled] = categoryOccurArr.filter(categoryOccur => categoryOccur.categoryType.equals("tags")).map(categoryOccur => CategoryInstalled(categoryOccur.categoryKey, categoryOccur.occurs))
      UserCategoryInstalled(guid, categoryUsageArr, subCategoryUsageArr, tagUsageArr)
    }.toDS
    //userCategoryUsageDF.sample(true, 0.2).show(10, false)
    val joinUseageDF = applistDF.join(userCategoryUsageDF, applistDF("guid") === userCategoryUsageDF("guid"), "left_outer")
      .select(applistDF("guid"), applistDF("installed_applist"), userCategoryUsageDF("categoryUsage"), userCategoryUsageDF("subCategoryUsage"), userCategoryUsageDF("tagUsage"))

    return joinUseageDF
    //println(userCategoryOccursStrRdd.take(5).mkString("\n"))
    //categoryOccursDF.rdd.map(row => row.getString(0))
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

  def loadAppPkgMetaDataFrame(path: String, sparkSession: SparkSession): Dataset[AppMeta] = {
    import sparkSession.implicits._
    val appMetaDF: DataFrame = sparkSession.read.option("delimiter", "\t").format("csv").csv(path)
    appMetaDF.flatMap{row =>
      Seq[AppMeta](AppMeta(row.getString(2), row.getString(5), row.getString(7), row.getString(10)),
        AppMeta(row.getString(3), row.getString(5), row.getString(7), row.getString(10))
      )
    }
  }

  def initAppMetaTable(path: String, spark: SparkSession):Unit = {
    import spark.implicits._
    val appMetaDF: DataFrame = spark.read.option("delimiter", "\t").format("csv").csv(path)
    appMetaDF.createOrReplaceTempView("dm_swa_app_tag_raw")
    spark.sql("select _c2 as app_name, _c3 as app_pkg, _c5 as category, _c7 as subCategory, _c10 as tags from dm_swa_app_tag_raw")
      .persist(StorageLevel.MEMORY_AND_DISK).createOrReplaceTempView(DM_APP_CATEGORY_TAGS)

    //spark.sql("select * from dm_app_category_tags ").show(3, false)
  }


  def loadInstallAppDataFrame(date: String, pathPattern: String, sparkSession: SparkSession): DataFrame = {
    val json_exists_paths = new JsonFilePathGenerator(pathPattern, date, sparkSession).getInstalledAppJsonFilePaths

    val jsonDF = sparkSession.read.format("json").json(json_exists_paths:_*)
    val applistDF: DataFrame = jsonDF.select("guid", "installed_applist").filter(row => !row.isNullAt(0))

    //val guidTagDF: Dataset[(String, String)] = applistDF.flatMap(row => row.getSeq[String](1).map((_, row.getString(0))))
    /*val guidTagAppDF: Dataset[GuidInstalledPkg] = applistDF.flatMap{row =>
      val guid: String = row.getString(0).trim
      row.getSeq[String](1).map(pkg => GuidInstalledPkg(guid, pkg.trim))
    }*/

    applistDF
  }

  def loadOpenAppDataFrame(date: String, pathPattern: String, sparkSession: SparkSession): DataFrame = {
    val json_exists_paths = new JsonFilePathGenerator(pathPattern, date, sparkSession).getOpenedAppJsonFilePaths
    val jsonDF = sparkSession.read.format("json").json(json_exists_paths:_*)
    val openAppDF = jsonDF.select("guid", "app_name", "open_time_and_ip").filter(row => !row.isNullAt(0))
    openAppDF
  }

}

case class GuidInstalledApp(guid: String, appPkg: String)
case class AppMeta(pkg: String, category: String, subCategory: String, tags: String)

case class CategoryInstalled(category: String, occurs: Int)
case class UserCategoryInstalled(guid: String, categoryUsage: Array[CategoryInstalled], subCategoryUsage: Array[CategoryInstalled], tagUsage: Array[CategoryInstalled])
