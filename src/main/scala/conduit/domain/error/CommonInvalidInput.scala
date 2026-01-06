package conduit.domain.error

import conduit.domain.error.ValidationError

import scala.util.matching.Regex

/**
 * Enum representing common validation errors used across the application.
 *
 * This enum defines validation errors that are not specific to any single entity
 * but are applicable to various inputs throughout the Conduit application.
 * These errors handle common validation constraints like string emptiness,
 * positive numbers, and UUID format validation.
 */
enum CommonInvalidInput extends ValidationError.InvalidInput:
  /**
   * Error indicating that a required string is empty.
   */
  case EmptyString

  /**
   * Error indicating that a numeric value must be positive.
   *
   * @param i the non-positive value that was provided
   */
  case NotPositive(i: Long)

  /**
   * Error indicating that a UUID string is invalid.
   *
   * @param id the invalid UUID string
   */
  case InvalidUUID(id: String)

  /**
   * Error indicating that a string does not match the required pattern.
   *
   * @param regex the regular expression pattern that the string should match
   */
  case InvalidString(regex: Regex)

  /**
   * Returns a human-readable message describing the validation error.
   *
   * @return the error message corresponding to this validation error case
   */
  override def message: String = this match {
    case NotPositive(i)       => "Value shoud be positive, $i given"
    case EmptyString          => "the given string is empty"
    case InvalidUUID(id)      => s"The given UUID $id is invalid"
    case InvalidString(regex) => s"The given string does not match with $regex"
  }
