package ai.starlake.schema.generator

import ai.starlake.TestHelper
import ai.starlake.utils.Utils

import scala.util.{Failure, Success}

class Yml2DDLSpec extends TestHelper {

  "Infer Create BQ Table" should "succeed" in {
    import org.slf4j.impl.StaticLoggerBinder
    val binder = StaticLoggerBinder.getSingleton
    logger.debug(binder.getLoggerFactory.toString)
    logger.debug(binder.getLoggerFactoryClassStr)

    new WithSettings() {
      new SpecTrait(
        sourceDomainOrJobPathname = "/sample/position/position.sl.yml",
        datasetDomainName = "position",
        sourceDatasetPathName = "/sample/position/XPOSTBL"
      ) {
        val schemaHandler = settings.schemaHandler()
        cleanMetadata
        deliverSourceDomain()
        deliverSourceTable(
          "position",
          "/sample/position/account_position.sl.yml",
          Some("account.sl.yml")
        )
        val config = Yml2DDLConfig("bigquery")
        val result = new Yml2DDLJob(config, schemaHandler).run()
        Utils.logFailure(result, logger) match {
          case Failure(exception) => throw exception
          case Success(_)         => // do nothing
        }
      }

    }
  }
}
