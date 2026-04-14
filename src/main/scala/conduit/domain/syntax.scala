package conduit.domain

import conduit.domain.error.{ ApplicationError, ValidationError }
import kyo.*
import zio.prelude.Validation

/**
 * Domain-specific syntax extensions and type aliases for the Conduit application.
 *
 * This object provides convenient syntax extensions and utility functions that make
 * working with validation, error handling, and effects more ergonomic throughout
 * the domain layer. It includes extensions for validation composition, error handling,
 * and effect transformations.
 */
object syntax:
  /**
   * Type alias for validation results with accumulated ArticleInvalidInput errors.
   *
   * This type represents the result of validation operations that can either
   * succeed with a value of type A or fail with accumulated validation errors.
   */
  type Validated[A] = Validation[ValidationError.InvalidInput, A]

  /**
   * Extension method to lift a value into a general effect type.
   *
   * This method allows any value to be treated as an effect that produces
   * that value without any additional context or effects.
   */
  extension [A](value: A) {
    def lift: A < Any = value
  }

  /**
   * Extension methods for validated effects that can abort with application errors.
   *
   * These methods provide convenient operations for working with validation results
   * in an effectful context, allowing composition and transformation of validated values.
   */
  extension [S, A](validated: Validated[A] < (S & Abort[ApplicationError])) {

    /**
     * Converts a validated effect to a pure effect, aborting on validation failure.
     *
     * @return the validated value on success, or aborts with ValidationError on failure
     */
    def validOrAbort: A < (S & Abort[ApplicationError]) = validated.map {
      case Validation.Success(_, value)  => value
      case Validation.Failure(_, errors) => Abort.fail(ValidationError(errors))
    }

    /**
     * Maps the validated value through an effectful function, preserving validation state.
     *
     * @param fn the function to apply to the validated value
     * @return a new validated effect with the transformed value
     */
    def traverse[B](
      fn: A => B < (S & Abort[ApplicationError])
    ): Validated[B] < (S & Abort[ApplicationError]) = validated.map {
      case Validation.Failure(logs, errors) => Validation.Failure(logs, errors)
      case Validation.Success(_, value)     => fn(value).map(Validation.succeed)
    }

    /**
     * FlatMaps the validated value through a function that returns another validation.
     *
     * @param fn the function that produces another validated effect
     * @return a new validated effect with the chained validation result
     */
    def flatTraverse[B](
      fn: A => Validated[B] < (S & Abort[ApplicationError])
    ): Validated[B] < (S & Abort[ApplicationError]) = validated.map {
      case Validation.Failure(logs, errors) => Validation.Failure(logs, errors)
      case Validation.Success(_, value)     => fn(value)
    }
  }

  /**
   * Extension methods for Maybe effects that can abort with application errors.
   *
   * These methods provide convenient ways to work with optional values in an
   * effectful context, particularly for converting absent values to specific errors.
   */
  extension [S, A](effect: Maybe[A] < (S & Abort[ApplicationError])) {

    /**
     * Converts a Maybe effect to a pure effect, aborting with a specific error if absent.
     *
     * @param err the error to throw if the Maybe is absent
     * @return the present value or aborts with the specified error
     */
    def ?!(err: ApplicationError): A < (S & Abort[ApplicationError]) = effect.map {
      case Maybe.Present(value) => value
      case Maybe.Absent         => Abort.fail(err)
    }
  }

  /**
   * Extension methods for general effects that can abort with application errors.
   *
   * These methods provide utilities for effect transformation, error handling,
   * and panic recovery in the context of application errors.
   */
  extension [S, A](effect: A < (S & Abort[ApplicationError])) {

    /**
     * Transforms the effect result to a different value, discarding the original.
     *
     * @param other the value to return instead of the effect result
     * @return an effect that produces the specified value
     */
    def as[B](other: B): B < (S & Abort[ApplicationError]) =
      effect.map(_ => other)

    /**
     * Recovers from panics by converting them to application errors.
     *
     * @param mapping the function to convert exceptions to application errors
     * @return an effect that handles panics as application errors
     */
    def unpanicWith(mapping: Throwable => ApplicationError): A < (S & Abort[ApplicationError]) =
      effect.unpanic.mapAbort {
        case exc: Throwable        => mapping(exc)
        case err: ApplicationError => err
      }
  }
