package com.ebiznext.comet.job.metrics

import com.ebiznext.comet.config.Settings
import com.ebiznext.comet.schema.handlers.{SchemaHandler, StorageHandler}
import com.ebiznext.comet.schema.model._
import com.ebiznext.comet.utils._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.lit

import scala.util.{Success, Try}

case class AssertionReport(
  name: String,
  params: String,
  sql: Option[String],
  countFailed: Option[Long],
  message: Option[String],
  success: Boolean
) {

  override def toString: String = {
    s"""name: $name, params:$params, countFailed:${countFailed.getOrElse(
      0
    )}, success:$success, message: ${message.getOrElse("")}, sql:$sql""".stripMargin
  }
}

/** Record assertion execution
  */

/** @param domain         : Domain name
  * @param schema         : Schema
  * @param stage          : stage
  * @param storageHandler : Storage Handler
  */
class AssertionJob(
  domain: Domain,
  schema: Schema,
  stage: Stage,
  storageHandler: StorageHandler,
  schemaHandler: SchemaHandler,
  dataset: DataFrame
)(implicit val settings: Settings)
    extends SparkJob {

  override def name: String = "Check Assertions"

  override def run(): Try[JobResult] = {
    val count = dataset.count()
    schema.assertions.foreach { assertions =>
      dataset.createOrReplaceTempView("comet_table")
      val assertionLibrary = schemaHandler.assertions
      val calls = AssertionDefinitions(assertions).assertionDefinitions
      val assertionReports = calls.map { case (k, assertion) =>
        val definition = assertionLibrary.get(k)
        val sql = definition.map(_.subst(assertion.params))
        try {
          val count = session
            .sql(
              sql.getOrElse(
                throw new IllegalArgumentException("Assertion not found in assertion library")
              )
            )
            .count()
          AssertionReport(
            assertion.name,
            assertion.params.toString(),
            sql,
            Some(count),
            None,
            true
          )
        } catch {
          case e: IllegalArgumentException =>
            AssertionReport(
              assertion.name,
              assertion.params.toString(),
              None,
              None,
              Some(Utils.exceptionAsString(e)),
              false
            )
          case e: Exception =>
            AssertionReport(
              assertion.name,
              assertion.params.toString(),
              sql,
              None,
              Some(Utils.exceptionAsString(e)),
              false
            )
        }
      }.toList

      assertionReports.foreach(r => logger.info(r.toString))

      val assertionsDF = session
        .createDataFrame(assertionReports)
        .withColumn("jobId", lit(settings.comet.jobId))
        .withColumn("domain", lit(domain.name))
        .withColumn("schema", lit(schema.name))
        .withColumn("count", lit(count))
        .withColumn("cometTime", lit(System.currentTimeMillis()))
        .withColumn("cometStage", lit(Stage.UNIT))
      val result = new SinkUtils().sinkMetrics(
        settings.comet.assertions.sink,
        assertionsDF,
        settings.comet.assertions.sink.name.getOrElse("assertions")
      )
    }
    Success(SparkJobResult(None))
  }

}
