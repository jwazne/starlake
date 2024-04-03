package ai.starlake.schema.generator

import ai.starlake.TestHelper
import ai.starlake.schema.handlers.SchemaHandler
import ai.starlake.schema.model.Domain
import ai.starlake.utils.YamlSerde
import better.files.File

class Yml2XlsSpec extends TestHelper {
  "Yml2XLS" should "should generated all domain / schema in XLS files" in {
    new WithSettings() {
      new SpecTrait(
        sourceDomainOrJobPathname = "/sample/position/position.sl.yml",
        datasetDomainName = "position",
        sourceDatasetPathName = "/sample/position/XPOSTBL"
      ) {
        cleanMetadata
        deliverSourceDomain()
        val schemaHandler = new SchemaHandler(settings.storageHandler())
        new Yml2Xls(schemaHandler).generateXls(Nil, "/tmp")
        val reader = new XlsDomainReader(InputPath("/tmp/position.xlsx"))
        val domain: Option[Domain] = reader.getDomain()
        assert(domain.isDefined)
        domain.foreach { domain =>
          assert(domain.name == "position")
          assert(domain.tables.size == 1)
          val accountSchema = domain.tables.filter(_.name == "account")
          assert(accountSchema.size == 1)
          accountSchema.head.attributes.size == 10
        }
      }
    }
  }

  new WithSettings() {
    new SpecTrait(
      sourceDomainOrJobPathname = "/sample/position/position.sl.yml",
      datasetDomainName = "position",
      sourceDatasetPathName = "/sample/position/XPOSTBL"
    ) {
      "a complex attribute list(aka JSON/XML)" should "produce the correct XLS file" in {
        val yamlPath =
          File(getClass.getResource("/sample/SomeComplexDomainTemplate.sl.yml"))
        val yamlDomain = YamlSerde
          .deserializeYamlLoadConfig(yamlPath.contentAsString, yamlPath.pathAsString)
          .getOrElse(throw new Exception(s"Invalid file name $yamlPath"))
        val schemaHandler = new SchemaHandler(settings.storageHandler())
        new Yml2Xls(schemaHandler).writeDomainXls(yamlDomain, "/tmp")
        val xlsOut = File("/tmp", yamlDomain.name + ".xlsx")
        val complexReader =
          new XlsDomainReader(InputPath(xlsOut.pathAsString))
        val xlsTable = complexReader.getDomain().get.tables.head
        val yamlTable = yamlDomain.tables.head
        xlsTable.attributes.length shouldBe yamlTable.attributes.length

        deepEquals(xlsTable.attributes, yamlTable.attributes)
      }
    }
  }

  "All SchemaGen Config" should "be known and taken  into account" in {
    val rendered = Yml2XlsCmd.usage()
    println(rendered)

    val expected =
      """
        |Usage: starlake yml2xls [options]
        |
        |  --domain <value>  domains to convert to XLS
        |  --iamPolicyTagsFile <value>
        |                           IAM PolicyTag file to convert to XLS, SL_METADATA/iam-policy-tags.yml by default)
        |  --xls <value>     directory where XLS files are generated
        |""".stripMargin
    rendered.substring(rendered.indexOf("Usage:")).replaceAll("\\s", "") shouldEqual expected
      .replaceAll("\\s", "")

  }

}
