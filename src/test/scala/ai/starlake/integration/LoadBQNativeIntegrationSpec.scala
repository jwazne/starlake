package ai.starlake.integration

import ai.starlake.job.Main

class LoadBQNativeIntegrationSpec extends BigQueryIntegrationSpecBase {
  override def templates = starlakeDir / "samples"
  override def localDir = templates / "spark"
  override def sampleDataDir = localDir / "sample-data"
  if (sys.env.getOrElse("SL_REMOTE_TEST", "false").toBoolean) {
    "Import / Load / Transform BQ NATIVE" should "succeed" in {
      withEnvs(
        "SL_ROOT" -> localDir.pathAsString,
        "SL_ENV"  -> "BQ-NATIVE"
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
    "Import / Load / Transform BQ NATIVE2" should "succeed" in {
      withEnvs(
        "SL_ROOT" -> localDir.pathAsString,
        "SL_ENV"  -> "BQ-NATIVE"
      ) {
        val sampleDataDir2 = localDir / "sample-data2"
        sampleDataDir2.copyTo(incomingDir)

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
