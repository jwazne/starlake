package ai.starlake.job

import ai.starlake.config.Settings
import ai.starlake.schema.handlers.{SchemaHandler, ValidateConfig}
import ai.starlake.utils.JobResult
import scopt.OParser

import scala.util.Try

trait CheckValidityCmd extends Cmd[CheckValidityCommand] {

  def command = "validate"

  val parser: OParser[Unit, CheckValidityCommand] = {
    val builder = OParser.builder[CheckValidityCommand]
    OParser.sequence(
      builder.programName(s"$shell $command"),
      builder.head(shell, command, "[options]"),
      builder.note(""),
      builder
        .opt[Boolean]("tables")
        .action((x, c) => c.copy(tables = x))
        .required()
        .text("Should we validate tables"),
      builder
        .opt[Boolean]("tasks")
        .action((_, c) => c.copy(tasks = true))
        .optional()
        .text("Should we validate tasks")
    )
  }

  def parse(args: Seq[String]): Option[CheckValidityCommand] = {
    OParser.parse(parser, args, CheckValidityCommand(), setup)
  }

  override def run(config: CheckValidityCommand, schemaHandler: SchemaHandler)(implicit
    settings: Settings
  ): Try[JobResult] =
    Try {
      schemaHandler.checkValidity(ValidateConfig(config.reload))
      JobResult.empty
    }
}

object CheckValidityCmd extends CheckValidityCmd
