package conduit.application.http.errors

import conduit.domain.error.NotFound.*
import conduit.domain.error.Unauthorised.*
import conduit.domain.error.{ NotFound => NotFoundError, * }
import sttp.model.StatusCode
import sttp.model.StatusCode.*

/**
 * Represents an error response to be sent to the client.
 *
 * @param status the HTTP status code of the error response
 * @param errors a map of error keys to lists of error messages, representing the details of the error.
 */
case class ErrorResponse(status: StatusCode, errors: Map[String, List[String]])

object ErrorResponse {

  /**
   * Encodes an ApplicationError into an ErrorResponse,
   * mapping specific error types to appropriate HTTP status codes and error messages.
   *
   * @param error the ApplicationError to be encoded into an ErrorResponse
   * @return an ErrorResponse representing the given ApplicationError, with an appropriate HTTP status code and error messages.
   */
  def encode(error: ApplicationError): ErrorResponse = error match
    case e: ArticleDeleteDenied.type             => ErrorResponse(Forbidden, Map("article" -> List("forbidden")))
    case e: ArticleUpdateDenied.type             => ErrorResponse(Forbidden, Map("article" -> List("forbidden")))
    case e: CommentDeleteDenied.type             => ErrorResponse(Forbidden, Map("comment" -> List("forbidden")))
    case e: ArticleNotFound                      => ErrorResponse(NotFound, Map("article" -> List("not found")))
    case e: ProfileNotFound                      => ErrorResponse(NotFound, Map("profile" -> List("not found")))
    case e: CommentNotFound                      => ErrorResponse(NotFound, Map("comment" -> List("not found")))
    case e: ApplicationError.NotFoundError       => ErrorResponse(NotFound, Map(e.kind -> List(e.message)))
    case e: Unauthorised.InvalidCredentials.type => ErrorResponse(Unauthorized, Map("credentials" -> List("invalid")))
    case e: ApplicationError.UnauthorisedError   => ErrorResponse(Forbidden, Map(e.kind -> List(e.message)))
    case e: ValidationError if isConflict(e)     => ErrorResponse(Conflict, encode(e.errors.toList))
    case e: ValidationError                      => ErrorResponse(UnprocessableEntity, encode(e.errors.toList))
    case e                                       => ErrorResponse(InternalServerError, Map(e.kind -> List(e.message)))

  /**
   * Encodes a ValidationError.InvalidInput into a tuple of (key, message) to be used in the error response.
   * This function maps specific invalid input cases to their corresponding error keys and messages.
   *
   * @param invalid the ValidationError.InvalidInput to be encoded
   * @return a tuple containing the error key and the corresponding error message for the given invalid input
   */
  def encode(invalid: ValidationError.InvalidInput): (String, String) = invalid match {
    case ArticleInvalidInput.EmptyTitle               => "title"       -> "can't be blank"
    case ArticleInvalidInput.EmptyBody                => "body"        -> "can't be blank"
    case ArticleInvalidInput.EmptyDescription         => "description" -> "can't be blank"
    case CommentInvalidInput.EmptyBody                => "body"        -> "can't be blank"
    case ProfileInvalidInput.EmptyUsername            => "username"    -> "can't be blank"
    case CredentialsInvalidInput.EmptyEmail           => "email"       -> "can't be blank"
    case CredentialsInvalidInput.EmptyPassword        => "password"    -> "can't be blank"
    case _: ProfileInvalidInput.UsernameAlreadyExists => "username"    -> "has already been taken"
    case _: CredentialsInvalidInput.EmailAlreadyInUse => "email"       -> "has already been taken"
    case invalid: ArticleInvalidInput                 => "article"     -> invalid.message
    case invalid                                      => invalid.kind  -> invalid.message
  }

  /**
   * Encodes a list of ValidationError.InvalidInput into a map of error keys to lists of error messages.
   * This function groups the encoded invalid inputs by their keys, allowing for multiple error messages under the same key.
   *
   * @param errors the list of ValidationError.InvalidInput to be encoded
   * @return a map where each key is an error key and the corresponding value is a list of error messages associated with that key
   */
  def encode(errors: List[ValidationError.InvalidInput]): Map[String, List[String]] =
    errors.map(encode).groupMap((key, _) => key)((_, message) => message)

  /**
   * Determines if a given ValidationError contains any errors that should be treated as conflicts (HTTP 409).
   * This function checks for specific error types that indicate a conflict, such as username already existing or email already in use.
   *
   * @param e the ValidationError to be checked for conflicts
   * @return true if the ValidationError contains any errors that should be treated as conflicts, false otherwise
   */
  private def isConflict(e: ValidationError): Boolean =
    e.errors.exists:
      case _: ProfileInvalidInput.UsernameAlreadyExists => true
      case _: CredentialsInvalidInput.EmailAlreadyInUse => true
      case _                                            => false

}
