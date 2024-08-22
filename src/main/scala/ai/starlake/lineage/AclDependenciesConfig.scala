package ai.starlake.lineage

import better.files.File

case class AclDependenciesConfig(
  grantees: List[String] = Nil,
  tables: List[String] = Nil,
  outputFile: Option[File] = None,
  reload: Boolean = false,
  svg: Boolean = false,
  png: Boolean = false,
  json: Boolean = false,
  all: Boolean = false
)
