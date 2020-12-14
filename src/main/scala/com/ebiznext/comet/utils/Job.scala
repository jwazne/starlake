package com.ebiznext.comet.utils

import com.ebiznext.comet.config.{Settings, SparkEnv, UdfRegistration}
import com.ebiznext.comet.schema.handlers.StorageHandler
import com.ebiznext.comet.schema.model.{Metadata, SinkType, Views}
import com.ebiznext.comet.schema.model.SinkType.{BQ, FS, JDBC}
import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.{DataFrame, DataFrameWriter, Row, SparkSession}
import com.ebiznext.comet.utils.Formatter._
import org.apache.hadoop.fs.Path

import scala.util.Try

trait JobResult

case class SparkJobResult(dataframe: Option[DataFrame]) extends JobResult

/** All Spark Job extend this trait.
  * Build Spark session using spark variables from application.conf.
  */

trait JobBase extends StrictLogging {
  def name: String
  implicit def settings: Settings

  /** Just to force any job to implement its entry point using within the "run" method
    *
    * @return : Spark Dataframe for Spark Jobs None otherwise
    */
  def run(): Try[JobResult]

}

trait SparkJob extends JobBase {

  lazy val sparkEnv: SparkEnv = {
    new SparkEnv(name)
  }

  lazy val session: SparkSession = {
    val udfs = settings.comet.udfs.map { udfs =>
      udfs.split(',').toList
    } getOrElse Nil
    udfs.foreach(registerUdf)
    sparkEnv.session
  }

  // TODO Should we issue a warning if used with Overwrite mode ????
  // TODO Check that the year / month / day / hour / minute do not already exist
  private def buildPartitionedDF(dataset: DataFrame, cols: List[String]): DataFrame = {
    var partitionedDF = dataset.withColumn("comet_date", current_timestamp())
    val dataSetsCols = dataset.columns.toList
    cols.foreach {
      case "comet_date" if !dataSetsCols.contains("date") =>
        partitionedDF = partitionedDF.withColumn(
          "date",
          date_format(col("comet_date"), "yyyyMMdd").cast(IntegerType)
        )
      case "comet_year" if !dataSetsCols.contains("year") =>
        partitionedDF = partitionedDF.withColumn("year", year(col("comet_date")))
      case "comet_month" if !dataSetsCols.contains("month") =>
        partitionedDF = partitionedDF.withColumn("month", month(col("comet_date")))
      case "comet_day" if !dataSetsCols.contains("day") =>
        partitionedDF = partitionedDF.withColumn("day", dayofmonth(col("comet_date")))
      case "comet_hour" if !dataSetsCols.contains("hour") =>
        partitionedDF = partitionedDF.withColumn("hour", hour(col("comet_date")))
      case "comet_minute" if !dataSetsCols.contains("minute") =>
        partitionedDF = partitionedDF.withColumn("minute", minute(col("comet_date")))
      case _ =>
        partitionedDF
    }
    partitionedDF.drop("comet_date")
  }

  /** Partition a dataset using dataset columns.
    * To partition the dataset using the ingestion time, use the reserved column names :
    *   - comet_date
    *   - comet_year
    *   - comet_month
    *   - comet_day
    *   - comet_hour
    *   - comet_minute
    * These columns are renamed to "date", "year", "month", "day", "hour", "minute" in the dataset and
    * their values is set to the current date/time.
    *
    * @param dataset   : Input dataset
    * @param partition : list of columns to use for partitioning.
    * @return The Spark session used to run this job
    */
  def partitionedDatasetWriter(
    dataset: DataFrame,
    partition: List[String]
  ): DataFrameWriter[Row] = {
    partition match {
      case Nil => dataset.write
      case cols if cols.forall(Metadata.CometPartitionColumns.contains) =>
        val strippedCols = cols.map(_.substring("comet_".length))
        val partitionedDF = buildPartitionedDF(dataset, cols)
        // does not work on nested fields -> https://issues.apache.org/jira/browse/SPARK-18084
        partitionedDF.write.partitionBy(strippedCols: _*)
      case cols if !cols.exists(Metadata.CometPartitionColumns.contains) =>
        dataset.write.partitionBy(cols: _*)
      case _ =>
        // Should never happend
        // TODO Test this at load time
        throw new Exception("Cannot mix comet & non comet col names")

    }
  }

  def partitionDataset(dataset: DataFrame, partition: List[String]): DataFrame = {
    logger.info(s"""Partitioning on ${partition.mkString(",")}""")
    partition match {
      case Nil => dataset
      case cols if cols.forall(Metadata.CometPartitionColumns.contains) =>
        buildPartitionedDF(dataset, cols)
      case cols if !cols.exists(Metadata.CometPartitionColumns.contains) =>
        dataset
      case _ =>
        dataset

    }
  }

  def analyze(fullTableName: String) = {
    if (settings.comet.analyze) {
      val allCols = session.table(fullTableName).columns.mkString(",")
      val analyzeTable =
        s"ANALYZE TABLE $fullTableName COMPUTE STATISTICS FOR COLUMNS $allCols"
      if (session.version.substring(0, 3).toDouble >= 2.4)
        try {
          session.sql(analyzeTable)
        } catch {
          case e: Throwable =>
            logger.warn(
              s"Failed to compute statistics for table $fullTableName on columns $allCols"
            )
            e.printStackTrace()
        }
    }
  }

  def createViews(
    views: Views,
    sqlParameters: Map[String, String],
    activeEnv: Map[String, String]
  ) = {
    // We parse the following strings
    //ex  BQ:[[ProjectID.]DATASET_ID.]TABLE_NAME"
    //or  BQ:[[ProjectID.]DATASET_ID.]TABLE_NAME.[comet_filter(col1 > 10 and col2 < 20)].[comet_select(col1, col2)]"
    //or  FS:/bucket/parquetfolder
    //or  JDBC:postgres:select *
    views.views.foreach { case (key, value) =>
      val valueWithEnv = Utils.subst(value, Nil, Nil, "comet_table", activeEnv)
      val sepIndex = valueWithEnv.indexOf(":")
      val (format, configName, path) =
        if (sepIndex > 0) {
          val key = valueWithEnv.substring(0, sepIndex)
          val sepConfigIndex = valueWithEnv.indexOf(':', sepIndex + 1)
          if (sepConfigIndex > 0) {
            (
              SinkType.fromString(valueWithEnv.substring(0, sepIndex)),
              Some(valueWithEnv.substring(sepIndex + 1, sepConfigIndex)),
              valueWithEnv.substring(sepConfigIndex + 1)
            )
          } else
            (SinkType.fromString(key), None, valueWithEnv.substring(sepIndex + 1))
        } else // parquet is the default
          (SinkType.FS, None, valueWithEnv)
      logger.info(s"Loading view $path from $format")
      val df = format match {
        case FS =>
          val fullPath =
            if (path.startsWith("/")) path else s"${settings.comet.datasets}/$path"
          session.read.parquet(fullPath)
        case JDBC =>
          val jdbcConfig =
            settings.comet.connections(configName.getOrElse((throw new Exception(""))))
          jdbcConfig.options
            .foldLeft(session.read)((w, kv) => w.option(kv._1, kv._2))
            .format(jdbcConfig.format)
            .option("query", path.richFormat(sqlParameters))
            .load()
            .cache()
        case BQ =>
          val TablePathWithFilter = "(.*)\\.comet_filter\\((.*)\\)".r
          val TablePathWithSelect = "(.*)\\.comet_select\\((.*)\\)".r
          val TablePathWithFilterAndSelect =
            "(.*)\\.comet_select\\((.*)\\)\\.comet_filter\\((.*)\\)".r
          path match {
            case TablePathWithFilterAndSelect(tablePath, select, filter) =>
              val filterFormat = filter.richFormat(sqlParameters)
              logger
                .info(s"We are loading the Table with columns: $select and filters: $filterFormat")
              session.read
                .option("readDataFormat", "AVRO")
                .format("com.google.cloud.spark.bigquery")
                .option("table", tablePath)
                .option("filter", filterFormat)
                .load()
                .selectExpr(select.replaceAll("\\s", "").split(","): _*)
                .cache()
            case TablePathWithFilter(tablePath, filter) =>
              val filterFormat = filter.richFormat(sqlParameters)
              logger.info(s"We are loading the Table with filters: $filterFormat")
              session.read
                .option("readDataFormat", "AVRO")
                .format("com.google.cloud.spark.bigquery")
                .option("table", tablePath)
                .option("filter", filterFormat)
                .load()
                .cache()
            case TablePathWithSelect(tablePath, select) =>
              logger.info(s"We are loading the Table with columns: $select")
              session.read
                .option("readDataFormat", "AVRO")
                .format("com.google.cloud.spark.bigquery")
                .option("table", tablePath)
                .load()
                .selectExpr(select.replaceAll("\\s", "").split(","): _*)
                .cache()
            case _ =>
              session.read
                .option("readDataFormat", "AVRO")
                .format("com.google.cloud.spark.bigquery")
                .option("table", path)
                .load()
                .cache()
          }
        case _ =>
          throw new Exception("Should never happen")
      }
      df.createOrReplaceTempView(key)
      logger.info(s"Created view $key")
    }
  }

  /** Saves a dataset. If the path is empty (the first time we call metrics on the schema) then we can write.
    *
    * If there's already parquet files stored in it, then create a temporary directory to compute on, and flush
    * the path to move updated metrics in it
    *
    * @param dataToSave :   dataset to be saved
    * @param path       :   Path to save the file at
    */
  def appendToFile(storageHandler: StorageHandler, dataToSave: DataFrame, path: Path): Unit = {
    if (storageHandler.exists(path)) {
      val pathIntermediate = new Path(path.getParent, ".tmp")

      logger.whenDebugEnabled {
        session.read.parquet(path.toString).show(false)
      }
      val dataByVariableStored: DataFrame = session.read
        .parquet(path.toString)
        .union(dataToSave)

      dataByVariableStored
        .coalesce(1)
        .write
        .mode("append")
        .parquet(pathIntermediate.toString)

      storageHandler.delete(path)
      storageHandler.move(pathIntermediate, path)
      logger.whenDebugEnabled {
        session.read.parquet(path.toString).show(1000, truncate = false)
      }
    } else {
      storageHandler.mkdirs(path)
      dataToSave
        .coalesce(1)
        .write
        .mode("append")
        .parquet(path.toString)

    }
  }

  def registerUdf(udf: String): Unit = {
    val udfInstance: UdfRegistration =
      Class
        .forName(udf)
        .getDeclaredConstructor()
        .newInstance()
        .asInstanceOf[UdfRegistration]
    udfInstance.register(session)
  }

}
