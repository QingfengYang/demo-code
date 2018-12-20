package com.lab.spark.applist

import java.sql.Struct

import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Column, DataFrame, SaveMode, SparkSession}
import org.apache.spark.storage.StorageLevel

object ApplisFeturesConvertApp {

  val DM_CATEGORY_TBL: String = "dm_app_category"
  val DM_SUB_CATEGORY_TBL: String = "dm_app_sub_category"
  val DM_TAG_TBL: String = "dm_app_tag"

  val STAT_APP_INSTALLED_TBL: String = "stat_app_installed_tbl"
  val STAT_APP_OPEN_TBL: String = "stat_app_open_tbl"
  val JOIN_MAP: String = "joinMap"

  def main(args: Array[String]): Unit = {
    if (args.length < 5) {
      val usage =
        """
          Usage: date, input_path_pattern, appMetaPath, hiveTable, mode
        """.stripMargin
    }
    // 20181120
    val date = args(0)
    //  /Users/yangqingfeng/tmp/debug/applist/date_p={DATE}/app_key_p={APP_KEY}/type_p={FILE_TYPE}/{FILE_TYPE}_{DATE}.txt
    val pathPattern = args(1)
    val appMetaPath = args(2)
    val hiveTable = args(3)
    val mode = args(4)

    val spark = if (mode.equals("local")) {
      println(s"run in local")
      SparkSession.builder().master("local[5]").appName("Applist Features").getOrCreate()
    } else {
      println(s"run in prod")
      SparkSession.builder().enableHiveSupport().appName("Applist Features").getOrCreate()
    }

    spark.udf.register("joinMap", (values: Seq[Map[String, Long]]) => {
      values.flatten.toMap
    })

    // 1. load app metadata
    initAppMetaTable(appMetaPath, spark)
    calculateInstallApp(spark, date, pathPattern)
    //calculateOpenAppDF(spark, date, pathPattern)
/*

    val resultTblDF : DataFrame = spark.sql(
      s"""
        select i.guid,
              installApp as install_app,
              installCategoryCount as install_category_count,
              installSubCategoryCount as install_subsategory_count,
              installTagCount as install_tag_count,
              openAppDays as open_app_days,
              openAppTimes as open_app_times,
              openCategoryDays as open_category_days,
              openCategoryTimes as open_category_times,
              openSubCategoryDays as open_subcategory_days,
              openSubCategoryTimes as open_subcategory_times,
              openTagDays as open_tag_days,
              openTagTimes as open_tag_times,
              $date as date_p
        from $STAT_APP_INSTALLED_TBL i left join $STAT_APP_OPEN_TBL o on i.guid=o.guid
      """.stripMargin)

    if (mode.equals("local")) {
      resultTblDF.printSchema()
      //resultTblDF.show(50, false)
    } else {
      resultTblDF.write.partitionBy("date_p").mode(SaveMode.ErrorIfExists).saveAsTable(hiveTable)
    }
*/

    spark.stop()
  }


  def calculateOpenAppDF(spark: SparkSession, date: String,
                         pathPattern: String): Unit = {
    val openAppDF = loadOpenAppDataFrame(date, pathPattern, spark)
    val app_open_tbl = "app_open_tbl"
    openAppDF.createOrReplaceTempView(s"$app_open_tbl")
    //openAppDF.show(3, false)

    val app_open_action_tbl = "app_open_action_tbl"
    spark.sql(s"select guid, appName, from_unixtime(cast(opt_time.open_time as bigint)/1000, 'yyyyMMdd') optTime from $app_open_tbl LATERAL VIEW explode(open_time_and_ip) opt_a AS opt_time ")
      .createOrReplaceTempView(s"$app_open_action_tbl")


    // 1. 按app统计使用频次
    spark.sql(
      s"""
          select guid, $JOIN_MAP(collect_list(map(appName, dayCount))) as openAppDays, $JOIN_MAP(collect_list(map(appName, times))) as openAppTimes
          from(
             select guid, a.appName as appName, count(distinct optTime) as dayCount, count(optTime) as times
               from $app_open_action_tbl a
               where a.appName is not null and length(a.appName) > 0
               group by guid, a.appName
             ) group by guid
       """.stripMargin).createOrReplaceTempView("user_app_open_stat_tbl")

    // 2. 按app类别计算使用频次
    // 2.1 category 统计
    spark.sql(
      s"""
          select guid, $JOIN_MAP(collect_list(map(category, openDayCount))) as openCategoryDays, $JOIN_MAP(collect_list(map(category, openTimes))) as openCategoryTimes
          from(
            select a.guid, m.category, count(distinct a.optTime) as openDayCount, count(optTime) as openTimes
              from $app_open_action_tbl a inner join $DM_CATEGORY_TBL m on a.appName=m.appName
              where category is not null and length(category) > 0
              group by guid, category
            ) group by guid
      """.stripMargin).createOrReplaceTempView("tmp_category_open_stat_final")

    // 2.2 subCategory 统计
    spark.sql(
      s"""
          select guid, $JOIN_MAP(collect_list(map(subCategory, openDayCount))) as openSubCategoryDays, $JOIN_MAP(collect_list(map(subCategory, openTimes))) as openSubCategoryTimes
          from(
             select a.guid, m.subCategory, count(distinct a.optTime) as openDayCount, count(optTime) as openTimes
               from $app_open_action_tbl a inner join $DM_SUB_CATEGORY_TBL m on a.appName=m.appName
                where subCategory is not null and length(subCategory) > 0
               group by guid, subCategory
           ) group by guid
      """.stripMargin).createOrReplaceTempView("tmp_subCategory_open_stat_final")

    // 3. 计算tags标签使用次数
    spark.sql(
      s"""
        select guid, $JOIN_MAP(collect_list(map(tag, openDayCount))) as openTagDays, $JOIN_MAP(collect_list(map(tag, openTimes))) as openTagTimes
        from (
          select guid, tag, count(distinct optTime) as openDayCount, count(optTime) as openTimes
          from $app_open_action_tbl a inner join $DM_TAG_TBL m on a.appName=m.appName
          group by guid, tag
        ) group by guid
      """.stripMargin).createOrReplaceTempView("tmp_tag_open_stat_final")

    spark.sql(
      """
        select a.guid, openAppDays, openAppTimes, openCategoryDays, openCategoryTimes, openSubCategoryDays, openSubCategoryTimes, openTagDays, openTagTimes
        from user_app_open_stat_tbl a
          left join tmp_category_open_stat_final c on a.guid=c.guid
          left join tmp_subCategory_open_stat_final s on a.guid=s.guid
          left join tmp_tag_open_stat_final t on a.guid=t.guid
      """.stripMargin).createOrReplaceTempView(STAT_APP_OPEN_TBL)

  }

  def calculateInstallApp(spark: SparkSession, date: String,
                          pathPattern: String): Unit = {
    val applistDF: DataFrame = loadInstallAppDataFrame(date, pathPattern, spark)
    val tmp_installed_applist = "tmp_installed_applist"
    applistDF.persist(StorageLevel.MEMORY_AND_DISK).createOrReplaceTempView(s"$tmp_installed_applist")

    import spark.implicits._
    import org.apache.spark.sql.functions._
    // @TODO debug
    applistDF.groupBy("guid").agg(count($"installed_applist") as "count").sort($"count".desc).show(10, false)
/*

    // 1. install apps
    val tmp_installed_app = "tmp_installed_app"
    spark.sql(
      s"""
        select guid, appName
        from tmp_installed_applist LATERAL VIEW explode(installed_applist) as appName
      """.stripMargin).createOrReplaceTempView("tmp_installed_app")

    // 2. category
    // 2.1 category install stat
    spark.sql(
      s"""
        select guid, $JOIN_MAP(collect_list(map(category, count))) as installCategoryCount
          from (
            select guid, category, count(distinct c.appName) as count
            from $tmp_installed_app i inner join $DM_CATEGORY_TBL c on i.appName=c.appName
            group by guid, category
          ) t
          group by guid
      """.stripMargin).createOrReplaceTempView("tmp_app_category_installed_final")


    // 2.2 subCategory install stat
    spark.sql(
      s"""
         select guid, $JOIN_MAP(collect_list(map(subCategory, count))) as installSubCategoryCount
                 from (
                   select guid, subCategory, count(distinct c.appName) as count
                   from $tmp_installed_app i inner join $DM_SUB_CATEGORY_TBL c on i.appName=c.appName
                   group by guid, subCategory
                 ) t
                 group by guid
      """.stripMargin).createOrReplaceTempView("tmp_app_subCategory_installed_final")

    // 3. tags installed
    spark.sql(
      s"""
         select guid, $JOIN_MAP(collect_list(map(tag, count))) as installTagCount
         from (
           select guid, tag, count(distinct tag) as count
           from $tmp_installed_app i inner join $DM_TAG_TBL t on i.appName=t.appName
           group by guid, tag
         ) t
         group by guid
       """.stripMargin).createOrReplaceTempView("tmp_app_tag_installed_final")

    // 4. final join all the data
    spark.sql(
      s"""
          select i.guid, installed_applist as installApp, installCategoryCount, installSubCategoryCount, installTagCount
          from $tmp_installed_applist i
            left join tmp_app_category_installed_final c on i.guid=c.guid
            left join tmp_app_subCategory_installed_final s on i.guid=s.guid
            left join tmp_app_tag_installed_final t on i.guid=t.guid
       """.stripMargin).createOrReplaceTempView(s"$STAT_APP_INSTALLED_TBL")
*/

    //spark.sql(s"select * from tmp_app_category_installed_final").printSchema()
  }

  def initAppMetaTable(path: String, spark: SparkSession):Unit = {
    import spark.implicits._
    val DM_APP_CATEGORY_TAGS_TBL: String = "dm_app_category_tags"
    val appMetaDF: DataFrame = spark.read.option("delimiter", "\t").format("csv").csv(path)
    appMetaDF.createOrReplaceTempView("dm_swa_app_tag_raw")
    val dmCategoryTagsMetaDF:DataFrame = spark.sql("select _c2 as appName, _c3 as appPkg, _c5 as category, _c7 as subCategory, _c10 as tags from dm_swa_app_tag_raw")
      .persist(StorageLevel.MEMORY_AND_DISK)
    dmCategoryTagsMetaDF.persist(StorageLevel.MEMORY_AND_DISK).createOrReplaceTempView(DM_APP_CATEGORY_TAGS_TBL)

    spark.sql(
      s"""
        select appName, appPkg, category
        from $DM_APP_CATEGORY_TAGS_TBL
        where category is not null and length(category) > 0
      """.stripMargin).persist(StorageLevel.MEMORY_AND_DISK).createOrReplaceTempView(DM_CATEGORY_TBL)

    spark.sql(
      s"""
        select appName, appPkg, subCategory
        from $DM_APP_CATEGORY_TAGS_TBL
        where subCategory is not null and length(subCategory) > 0
      """.stripMargin).persist(StorageLevel.MEMORY_AND_DISK).createOrReplaceTempView(DM_SUB_CATEGORY_TBL)

    spark.sql(s"select appName, split(tags, ',') as tag_array from $DM_APP_CATEGORY_TAGS_TBL where tags is not null and length(tags) > 0 ").createOrReplaceTempView("app_tags_meta")
    spark.sql("select appName, tag from app_tags_meta LATERAL VIEW explode(tag_array) tag_tbl as tag ").createOrReplaceTempView(DM_TAG_TBL)

    dmCategoryTagsMetaDF.unpersist()
    //spark.sql("select * from dm_app_category_tags ").show(3, false)
  }


  def loadInstallAppDataFrame(date: String, pathPattern: String, sparkSession: SparkSession): DataFrame = {
    val json_exists_paths = new JsonFilePathGenerator(pathPattern, date, sparkSession).getInstalledAppJsonFilePaths

    val jsonDF = sparkSession.read.format("json").json(json_exists_paths:_*)
    val applistDF: DataFrame = jsonDF.select("guid", "installed_applist").filter(row => !row.isNullAt(0))

    applistDF
  }

  def loadOpenAppDataFrame(date: String, pathPattern: String, sparkSession: SparkSession): DataFrame = {
    import sparkSession.implicits._
    val json_exists_paths = new JsonFilePathGenerator(pathPattern, date, sparkSession).getOpenedAppJsonFilePaths
    val jsonDF = sparkSession.read.format("json").json(json_exists_paths:_*)
    val openAppDF = jsonDF.select($"guid", $"app_name".alias("appName"), $"open_time_and_ip").filter(row => !row.isNullAt(0))
    openAppDF
  }

}
