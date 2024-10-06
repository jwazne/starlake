package ai.starlake.schema.generator

import ai.starlake.TestHelper

class Yml2DagTemplateLoaderSpec extends TestHelper {
  new WithSettings() {
    "Parse Jinja" should "should be able to extract template file and access all variables" in {
      val templates = new Yml2DagTemplateLoader().allLoadTemplates()
      for (elem <- templates) { println(elem) }
    }
  }
}
