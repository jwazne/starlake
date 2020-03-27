package com.ebiznext.comet.database.extractor

import better.files.File
import org.scalatest.matchers.should.Matchers

class ExtractScriptGenSpec extends org.scalatest.FlatSpec with Matchers {

  val scriptOutputFolder: File = File("/tmp")

  "templatize" should "generate an export script from a TemplateSettings" in {
    val templateParams: TemplateParams = TemplateParams(
      tableToExport = "table1",
      columnsToExport = List("col1", "col2"),
      fullExport = false,
      dsvDelimiter = ",",
      deltaColumn = Some("updateCol"),
      exportOutputFileBase = "output_file",
      scriptOutputFile = scriptOutputFolder / "EXTRACT_table1.sql"
    )

    val templatePayload: String = ScriptGen.templatize(
      File(
        getClass.getResource("/sample/database/EXTRACT_TABLE.sql.mustache").getPath
      ),
      templateParams
    )

    templatePayload shouldBe File(
      getClass.getResource("/sample/database/expected_script_payload.txt").getPath
    ).lines.mkString("\n")
  }

}
