package conduit.application.http.errors

import conduit.domain.error.NotFound.*
import conduit.domain.error.Unauthorised.*
import conduit.domain.error.{ NotFound => NotFoundError, * }
import sttp.model.StatusCode
import sttp.model.StatusCode.*

case class ErrorResponse(status: StatusCode, errors: Map[String, List[String]])

object ErrorResponse {
  def encode(error: ApplicationError): ErrorResponse = error match
    case e: ArticleDeleteDenied.type           => ErrorResponse(Forbidden, Map("article" -> List("forbidden")))
    case e: ArticleUpdateDenied.type           => ErrorResponse(Forbidden, Map("article" -> List("forbidden")))
    case e: CommentDeleteDenied.type           => ErrorResponse(Forbidden, Map("comment" -> List("forbidden")))
    case e: ArticleNotFound                    => ErrorResponse(NotFound, Map("article" -> List("not found")))
    case e: ProfileNotFound                    => ErrorResponse(NotFound, Map("profile" -> List("not found")))
    case e: CommentNotFound                    => ErrorResponse(NotFound, Map("comment" -> List("not found")))
    case e: ApplicationError.NotFoundError     => ErrorResponse(NotFound, Map(e.kind -> List(e.message)))
    case e: Unauthorised.InvalidCredentials.type => ErrorResponse(Unauthorized, Map("credentials" -> List("invalid")))
    case e: ApplicationError.UnauthorisedError   => ErrorResponse(Forbidden, Map(e.kind -> List(e.message)))
    case e: ValidationError if isConflict(e)   => ErrorResponse(Conflict, encode(e.errors.toList))
    case e: ValidationError                    => ErrorResponse(UnprocessableEntity, encode(e.errors.toList))
    case e                                     => ErrorResponse(InternalServerError, Map(e.kind -> List(e.message)))

  def encode(invalid: ValidationError.InvalidInput): (String, String) = invalid match {
    case ArticleInvalidInput.EmptyTitle               => ("title", "can't be blank")
    case ArticleInvalidInput.EmptyBody                => ("body", "can't be blank")
    case ArticleInvalidInput.EmptyDescription         => ("description", "can't be blank")
    case CommentInvalidInput.EmptyBody                => ("body", "can't be blank")
    case ProfileInvalidInput.EmptyUsername            => ("username", "can't be blank")
    case CredentialsInvalidInput.EmptyEmail           => ("email", "can't be blank")
    case CredentialsInvalidInput.EmptyPassword        => ("password", "can't be blank")
    case _: ProfileInvalidInput.UsernameAlreadyExists => ("username", "has already been taken")
    case _: CredentialsInvalidInput.EmailAlreadyInUse => ("email", "has already been taken")
    case invalid: ArticleInvalidInput                 => ("article", invalid.message)
    case invalid                                      => (invalid.kind, invalid.message)
  }

  def encode(errors: List[ValidationError.InvalidInput]): Map[String, List[String]] =
    errors.map(encode).groupMap((key, _) => key)((_, message) => message)

  private def isConflict(e: ValidationError): Boolean =
    e.errors.exists:
      case _: ProfileInvalidInput.UsernameAlreadyExists => true
      case _: CredentialsInvalidInput.EmailAlreadyInUse => true
      case _                                            => false

}
