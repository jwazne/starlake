package ai.starlake.integration

import ai.starlake.job.Main
import better.files.File

class LoadPgIntegrationSpec extends JDBCIntegrationSpecBase {
  override def templates: File = starlakeDir / "samples"
  override def localDir: File = templates / "spark"
  override def sampleDataDir: File = localDir / "sample-data"

  if (sys.env.getOrElse("SL_LOCAL_TEST", "false").toBoolean) {
    "Import / Load / Transform PG" should "succeed" in {
      withEnvs(
        "SL_ROOT" -> localDir.pathAsString,
        "SL_ENV"  -> "PG"
      ) {
        cleanup()
        copyFilesToIncomingDir(sampleDataDir)
        Main.run(
          Array("import")
        )
        Main.run(
          Array("load")
        )
      }
    }
    "Import / Load / Transform PG 2" should "succeed" in {
      withEnvs(
        "SL_ROOT" -> localDir.pathAsString,
        "SL_ENV"  -> "PG"
      ) {
        val sampleDataDir2 = localDir / "sample-data2"
        copyFilesToIncomingDir(sampleDataDir2)
        Main.run(
          Array("import")
        )
        Main.run(
          Array("load")
        )
      }
    }
  }
}
