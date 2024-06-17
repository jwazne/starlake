package ai.starlake.workflow

import better.files.File
import ai.starlake.TestHelper
import ai.starlake.config.DatasetArea
import org.apache.hadoop.fs.Path

import scala.io.Codec

class IngestionWorkflowSpec extends TestHelper {
  new WithSettings() {

    private def loadLandingFile(landingFile: String): TestHelper#SpecTrait = {
      new SpecTrait(
        sourceDomainOrJobPathname = "/sample/DOMAIN.sl.yml",
        datasetDomainName = "DOMAIN",
        sourceDatasetPathName = landingFile
      ) {
        cleanMetadata
        deliverSourceDomain()
        List(
          "/sample/User.sl.yml",
          "/sample/Players.sl.yml",
          "/sample/employee.sl.yml",
          "/sample/complexUser.sl.yml"
        ).foreach(deliverSourceTable)

        storageHandler.delete(new Path(landingPath))
        // Make sure unrelated files, even without extensions, are not imported
        storageHandler.touchz(new Path(landingPath, "_SUCCESS"))

        loadLanding(Codec.default, createAckFile = false)
        val destFolder: Path = DatasetArea.stage(datasetDomainName)
        assert(
          storageHandler.exists(new Path(destFolder, "SCHEMA-VALID.dsv")),
          "Landing file directly imported"
        )
        assert(
          !storageHandler.exists(new Path(destFolder, "_SUCCESS")),
          "Unrelated file ignored"
        )
      }

      // Test again, but with Domain.ack defined
      new SpecTrait(
        sourceDomainOrJobPathname = "/sample/DOMAIN-ACK.sl.yml",
        datasetDomainName = "DOMAIN",
        sourceDatasetPathName = landingFile
      ) {
        cleanMetadata
        deliverSourceDomain()
        deliverSourceTable("/sample/User.sl.yml")
        deliverSourceTable("DOMAIN", "/sample/employee_DOMAIN-ACK.sl.yml", Some("employee.sl.yml"))
        deliverSourceTable("DOMAIN", "/sample/Players_DOMAIN-ACK.sl.yml", Some("Players.sl.yml"))

        storageHandler.delete(new Path(landingPath))

        loadLanding
        val destFolder: Path = DatasetArea.stage(datasetDomainName)
        assert(
          storageHandler.exists(new Path(destFolder, "SCHEMA-VALID.dsv")),
          "Landing file based on extension imported"
        )
        val ackFilename: String = File(landingFile).nameWithoutExtension + ".ack"
        assert(
          !storageHandler.exists(new Path(destFolder, ackFilename)),
          "Ack file not imported"
        )
        assert(
          !storageHandler.exists(new Path(landingPath, ackFilename)),
          "Ack file removed from landing zone"
        )
      }
    }

    "Loading files in Landing area" should "produce file in pending area" in {
      loadLandingFile("/sample/SCHEMA-VALID.dsv")
    }

    "Loading zip files in Landing area" should "produce file contained in Zip File in pending area" in {
      loadLandingFile("/sample/SCHEMA-VALID.dsv.zip")
    }

    "Loading gzip files in Landing area" should "produce file contained in GZ File in pending area" in {
      loadLandingFile("/sample/SCHEMA-VALID.dsv.gz")
    }

    "Loading tgz files in Landing area" should "produce file contained in tgz File in pending area" in {
      loadLandingFile("/sample/SCHEMA-VALID.dsv.tgz")
    }
  }
}
