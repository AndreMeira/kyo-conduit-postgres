package conduit.domain.error

/**
 * Represents errors related to unauthorized actions or authentication failures.
 *
 * This enum covers cases such as invalid or expired tokens, invalid credentials,
 * invalid token subject, and denial of article updates due to insufficient permissions.
 * Each case provides a descriptive error message.
 */
enum Unauthorised extends ApplicationError.UnauthorisedError {

  /** 
   * The provided authentication token is invalid.
   * This error indicates that the token could not be verified or is malformed.
   */
  case InvalidToken

  /** 
   * The authentication token has expired.
   * This error indicates that the token is no longer valid due to time constraints.
   */
  case TokenExpired

  /** 
   * The provided credentials (email/password) are invalid.
   * This error indicates that the authentication attempt failed due to incorrect user input.
   */
  case InvalidCredentials

  /** 
   * The subject of the authentication token is invalid.
   * This error indicates that the token does not correspond to a valid user or entity.
   */
  case InvalidTokenSubject

  /** 
   * The user is not authorized to update the article.
   * This error indicates insufficient permissions for the requested operation.
   */
  case ArticleUpdateDenied

  /**
   * The user is not authorized to delete the article.
   * This error indicates that the requester is not the author of the article.
   */
  case ArticleDeleteDenied

  /**
   * The user is not authorized to delete the comment.
   * This error indicates that the requester is not the author of the comment.
   */
  case CommentDeleteDenied

  /**
   * Returns a human-readable error message for each unauthorized error.
   */
  override def message: String = this match {
    case InvalidToken        => "Invalid token"
    case InvalidCredentials  => "Invalid email/password"
    case TokenExpired        => "Token has expired"
    case InvalidTokenSubject => "Invalid token subject"
    case ArticleUpdateDenied => "Article update denied"
    case ArticleDeleteDenied => "Article delete denied"
    case CommentDeleteDenied => "Comment delete denied"
  }
}
