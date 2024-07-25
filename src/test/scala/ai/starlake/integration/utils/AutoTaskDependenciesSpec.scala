package ai.starlake.integration.utils

import ai.starlake.integration.IntegrationTestBase
import ai.starlake.job.Main

class AutoTaskDependenciesSpec extends IntegrationTestBase {
  override def localDir = starlakeDir / "samples" / "starbake"
  override def sampleDataDir = localDir / "sample-data"
  logger.info(localDir.pathAsString)

  override def beforeEach(): Unit = {
    // do not clean
  }

  override def afterEach(): Unit = {
    // do not clean
  }

  "Autoload" should "succeed" in {
    withEnvs("SL_ROOT" -> localDir.pathAsString, "SL_ENV" -> "DUCKDB") {
      copyFilesToIncomingDir(sampleDataDir)
      assert(new Main().run(Array("autoload", "--clean")))
    }
  }

  "Recursive Transform" should "succeed" in {
    if (sys.env.getOrElse("SL_LOCAL_TEST", "true").toBoolean) {
      withEnvs("SL_ROOT" -> localDir.pathAsString, "SL_ENV" -> "DUCKDB") {
        val result = new Main().run(
          Array("transform", "--recursive", "--name", "kpi.order_summary")
        )
        assert(result)
      }
    }
  }

  "Dependencies" should "succeed" in {
    if (sys.env.getOrElse("SL_LOCAL_TEST", "true").toBoolean) {
      withEnvs("SL_ROOT" -> localDir.pathAsString, "SL_ENV" -> "DUCKDB") {
        assert(
          new Main().run(
            Array("acl-dependencies", "--all")
          )
        )
      }
    }
  }

  "Lineage" should "succeed" in {
    if (sys.env.getOrElse("SL_LOCAL_TEST", "true").toBoolean) {
      withEnvs(
        "SL_ROOT" -> localDir.pathAsString,
        "SL_ENV"  -> "DUCKDB"
      ) {
        assert(
          new Main().run(
            Array("lineage", "--viz", "--all")
          )
        )
      }
    }
  }

  "Lineage JSON" should "succeed" in {
    if (sys.env.getOrElse("SL_LOCAL_TEST", "true").toBoolean) {
      withEnvs(
        "SL_ROOT" -> localDir.pathAsString,
        "SL_ENV"  -> "DUCKDB"
      ) {
        assert(
          new Main().run(
            Array("lineage", "--json", "--print", "--all")
          )
        )
      }
    }
  }

  "Relations Generation" should "succeed" in {
    if (sys.env.getOrElse("SL_LOCAL_TEST", "true").toBoolean) {
      withEnvs(
        "SL_ROOT" -> localDir.pathAsString,
        "SL_ENV"  -> "DUCKDB"
      ) {
        assert(
          new Main().run(
            Array("table-dependencies")
          )
        )
      }
    }
  }

  "JSON Relations Generation" should "succeed" in {
    if (sys.env.getOrElse("SL_LOCAL_TEST", "true").toBoolean) {
      withEnvs(
        "SL_ROOT" -> (starlakeDir / "samples" / "spark").pathAsString,
        "SL_ENV"  -> "DUCKDB"
      ) {
        assert(
          new Main().run(
            Array("table-dependencies", "--json")
          )
        )
      }
    }
  }

  "Job GraphViz Generation" should "succeed" in {
    if (sys.env.getOrElse("SL_LOCAL_TEST", "true").toBoolean) {
      withEnvs(
        "SL_ROOT" -> localDir.pathAsString /* , "SL_METADATA" -> starbakeDir.pathAsString */
      ) {
        assert(
          new Main().run(
            Array(
              "lineage",
              "--print",
              "--viz",
              "--task",
              "kpi.order_summary",
              "--verbose"
            )
          )
        )
      }
    }
  }
}
