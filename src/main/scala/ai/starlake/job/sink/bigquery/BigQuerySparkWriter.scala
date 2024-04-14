package ai.starlake.job.sink.bigquery

import ai.starlake.config.Settings
import ai.starlake.schema.model._
import ai.starlake.utils.Utils
import com.google.cloud.bigquery.{Schema => BQSchema}
import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.sql.DataFrame

import scala.util.{Failure, Success, Try}
/*
BigQueryLoadConfig(
            Some(settings.comet.audit.getConnectionRef()),
            Right(rejectedDF),
            outputTableId = Some(
              BigQueryJobBase
                .extractProjectDatasetAndTable(
                  settings.comet.audit.database,
                  settings.comet.audit.getDomain(),
                  "rejected"
                )
            ),
 */
object BigQuerySparkWriter extends StrictLogging {
  def sinkInAudit(
    df: DataFrame,
    tableName: String,
    maybeTableDescription: Option[String],
    maybeSchema: Option[BQSchema],
    writeMode: WriteMode,
    accessToken: Option[String]
  )(implicit
    settings: Settings
  ): Try[Unit] = {
    Try {
      settings.appConfig.audit.sink.getSink() match {
        case sink: BigQuerySink =>
          val source = Right(Utils.setNullableStateOfColumn(df, nullable = true))
          val (createDisposition, writeDisposition) = {
            Utils.getDBDisposition(writeMode)
          }
          val bqLoadConfig =
            BigQueryLoadConfig(
              connectionRef = Some(settings.appConfig.audit.getConnectionRef()),
              source = source,
              outputTableId = Some(
                BigQueryJobBase
                  .extractProjectDatasetAndTable(
                    settings.appConfig.audit.getDatabase(),
                    settings.appConfig.audit.getDomain(),
                    tableName
                  )
              ),
              sourceFormat = settings.appConfig.defaultWriteFormat,
              createDisposition = createDisposition,
              writeDisposition = writeDisposition,
              outputPartition = sink.getPartitionColumn(),
              outputClustering = sink.clustering.getOrElse(Nil),
              days = sink.days,
              requirePartitionFilter = sink.requirePartitionFilter.getOrElse(false),
              rls = Nil,
              acl = Nil,
              outputDatabase = settings.appConfig.audit.getDatabase(),
              accessToken = accessToken
            )
          val result = new BigQuerySparkJob(
            bqLoadConfig,
            maybeBqSchema = maybeSchema,
            maybeTableDescription = maybeTableDescription
          ).run()
          result match {
            case Success(_) => ;
            case Failure(e) =>
              throw e
          }
        case _: EsSink =>
          // TODO Sink Audit Log to ES
          throw new Exception("Sinking Audit log to Elasticsearch not yet supported")
        case _: FsSink =>
        // Do nothing dataset already sinked to file. Forced at the reference.conf level
        case _ =>
      }
    }
  }
}
