package conduit.domain.service.validation

import conduit.domain.error.{ ValidationError, CommonInvalidInput as Invalid }
import conduit.domain.syntax.Validated
import zio.prelude.Validation

import java.util.UUID
import scala.util.Try
import scala.util.matching.Regex

/**
 * Common utilities shared across different domain validations.
 *
 * This object provides reusable validation functions for common data types and
 * constraints such as positive numbers, non-empty strings, UUIDs, and regex matching.
 * These functions are building blocks used by specialized validation services like
 * ArticleValidation and CommentValidation.
 */
object CommonValidation {

  /**
   * Attempts to execute a block of code that may throw an exception and maps any thrown
   * exception to a ValidationError using the provided mapping function.
   *
   * @param value the block of code to execute
   * @param mapError a function that maps a Throwable to a ValidationError.InvalidInput
   * @return a Validated containing the result of the block or a validation error
   */
  def attempt[A](value: => A)(mapError: Throwable => ValidationError.InvalidInput): Validated[A] =
    Validation
      .fromTry(Try(value))
      .mapError(mapError)

  /**
   * Validates that an integer value is positive (>= 0).
   *
   * @param value the integer to validate
   * @return a validated positive integer or NotPositive error
   */
  def positive(value: Int): Validated[Int] =
    if value >= 0 then Validation.succeed(value)
    else Validation.fail(Invalid.NotPositive(value))

  /**
   * Validates that a long value is positive (>= 0).
   *
   * @param value the long to validate
   * @return a validated positive long or NotPositive error
   */
  def positive(value: Long): Validated[Long] =
    if value >= 0 then Validation.succeed(value)
    else Validation.fail(Invalid.NotPositive(value))

  /**
   * Validates and parses a UUID string.
   *
   * @param id the string representation of a UUID
   * @return a validated UUID or InvalidUUID error
   */
  def uuid(id: String): Validated[UUID] =
    attempt(UUID.fromString(id)): _ =>
      Invalid.InvalidUUID(id)

  /**
   * Validates that a string is non-empty after trimming whitespace.
   *
   * @param value the string to validate
   * @return a validated non-empty trimmed string or EmptyString error
   */
  def nonEmptyString(value: String): Validated[String] =
    value match {
      case ""  => Validation.fail(Invalid.EmptyString)
      case str => Validation.succeed(str)
    }

  def length(value: String, min: Int, max: Int): Validated[String] =
    val len = value.length
    if len >= min && len <= max then Validation.succeed(value)
    else Validation.fail(Invalid.InvalidLength(value, min, max))

  /**
   * Validates that a string is non-empty and matches a given regex pattern.
   *
   * @param value the string to validate
   * @param regex the regular expression pattern to match against
   * @return a validated string matching the pattern or InvalidString error
   */
  def nonEmptyMatch(value: String, regex: Regex): Validated[String] =
    value match {
      case str if regex.matches(str) => Validation.succeed(str)
      case _                         => Validation.fail(Invalid.InvalidString(regex))
    }

  /**  
   * Validates that all provided values are the same.
   *
   * @param head the first value to compare
   * @param tail the remaining values to compare against the head
   * @return a validated value if all are the same or DifferentValues error
   */
  def sameValues[A](head: A, tail: A*): Validated[A] =
    if tail.forall(_ == head) then Validation.succeed(head)
    else Validation.fail(Invalid.DifferentValues)
}
