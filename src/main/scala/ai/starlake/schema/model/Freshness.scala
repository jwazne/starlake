package ai.starlake.schema.model

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import ai.starlake.schema.model.Severity._

case class Freshness(
  warn: Option[String] = None,
  error: Option[String] = None
) {
  override def toString: String = {
    val warnStr = warn.getOrElse("")
    val errorStr = error.getOrElse("")
    s"Freshness(warn=$warnStr, error=$errorStr)"
  }
  def checkValidity(): Either[List[ValidationMessage], Boolean] = {
    val errorList: mutable.ListBuffer[ValidationMessage] = mutable.ListBuffer.empty

    def checkDuration(duration: Option[String]): Unit = {
      Try {
        duration.map(Duration(_).toSeconds)
      } match {
        case Failure(_) =>
          errorList += ValidationMessage(
            Error,
            "Freshness",
            s"duration: $duration could not be parsed as a duration"
          )
        case Success(_) =>
      }
    }

    checkDuration(warn)
    checkDuration(error)
    if (errorList.isEmpty)
      Right(true)
    else
      Left(errorList.toList)
  }
}
