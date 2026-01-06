package conduit.domain

import conduit.domain.error.{ ApplicationError, ValidationError }
import kyo.*
import zio.prelude.Validation

object syntax:
  type Validated[A] = Validation[ValidationError.InvalidInput, A]

  // Helper dsl
  extension [S, A](validated: Validated[A] < (S & Abort[ApplicationError])) {
    def validOrAbort: A < (S & Abort[ApplicationError]) = validated.map {
      case Validation.Success(_, value)  => value
      case Validation.Failure(_, errors) => Abort.fail(ValidationError(errors))
    }

    def traverse[B](
      fn: A => B < (S & Abort[ApplicationError])
    ): Validated[B] < (S & Abort[ApplicationError]) = validated.map {
      case Validation.Failure(logs, errors) => Validation.Failure(logs, errors)
      case Validation.Success(_, value)     => fn(value).map(Validation.succeed)
    }

    def flatTraverse[B](
      fn: A => Validated[B] < (S & Abort[ApplicationError])
    ): Validated[B] < (S & Abort[ApplicationError]) = validated.map {
      case Validation.Failure(logs, errors) => Validation.Failure(logs, errors)
      case Validation.Success(_, value)     => fn(value)
    }
  }

  extension [S, A](effect: Maybe[A] < (S & Abort[ApplicationError])) {
    def ?!(err: ApplicationError): A < (S & Abort[ApplicationError]) = effect.map {
      case Maybe.Present(value) => value
      case Maybe.Absent         => Abort.fail(err)
    }
  }

  extension [S, A](effect: A < (S & Abort[ApplicationError])) {
    def as[B](other: B): B < (S & Abort[ApplicationError]) =
      effect.map(_ => other)

    def unpanicWith(mapping: Throwable => ApplicationError): A < (S & Abort[ApplicationError]) =
      effect.unpanic.mapAbort {
        case exc: Throwable        => mapping(exc)
        case err: ApplicationError => err
      }
  }
