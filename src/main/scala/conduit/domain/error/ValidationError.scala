package conduit.domain.error

import ApplicationError.DomainError
import zio.NonEmptyChunk

/**
 * Case class representing a collection of validation errors.
 *
 * This class aggregates one or more validation errors that occur during input validation.
 * It allows multiple validation failures to be reported together, providing a comprehensive
 * view of all validation constraints that were violated.
 *
 * @param errors a non-empty collection of validation errors
 */
case class ValidationError(errors: NonEmptyChunk[ValidationError.InvalidInput]) extends DomainError:
  /**
   * Returns a human-readable message combining all validation errors.
   *
   * @return a comma-separated string of all validation error messages
   */
  override def message: String = errors.map(_.toString).mkString(", ")

object ValidationError:
  /**
   * Trait for individual validation input errors.
   *
   * This trait defines the contract for specific validation constraint violations.
   * Each implementation represents a single validation rule that was broken during
   * input validation.
   */
  trait InvalidInput:
    /**
     * Returns a human-readable message describing the validation error.
     *
     * @return the error message
     */
    def message: String

    /**
     * Returns the kind/type of validation error based on the class name.
     *
     * @return the error kind as a string derived from the class name
     */
    def kind: String              = getClass.getSimpleName

    /**
     * Returns a string representation of the validation error.
     *
     * @return a formatted string containing the error kind and message
     */
    override def toString: String = s"{kind: $kind, message: $message}"
