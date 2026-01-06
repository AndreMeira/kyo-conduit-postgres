package conduit.domain.service.validation

import conduit.domain.error.{ ValidationError, CommonInvalidInput as Invalid }
import conduit.domain.syntax.Validated
import zio.prelude.Validation

import java.util.UUID
import scala.util.Try
import scala.util.matching.Regex

object CommonValidation {

  def positive(value: Int): Validated[Int] =
    if value >= 0 then Validation.succeed(value)
    else Validation.fail(Invalid.NotPositive(value))

  def positive(value: Long): Validated[Long] =
    if value >= 0 then Validation.succeed(value)
    else Validation.fail(Invalid.NotPositive(value))

  def uuid(id: String): Validated[UUID] =
    Validation
      .fromTry(Try(UUID.fromString(id)))
      .asError(Invalid.InvalidUUID(id))

  def nonEmptyString(value: String): Validated[String] =
    value.trim match {
      case ""  => Validation.fail(Invalid.EmptyString)
      case str => Validation.succeed(str)
    }

  def nonEmptyMatch(value: String, regex: Regex): Validated[String] =
    value.trim match {
      case str if regex.matches(str) => Validation.succeed(str)
      case _                         => Validation.fail(Invalid.InvalidString(regex))
    }
}
