package conduit.domain.request.comment

import conduit.domain.error.ValidationError
import conduit.domain.model.Comment

/**
 * Enum representing validation errors specific to comment operations.
 *
 * This enum defines all possible validation errors that can occur when
 * creating or updating comments in the Conduit application. Each case
 * represents a specific validation constraint violation related to comment data.
 */
enum InvalidInput extends ValidationError.InvalidInput {

  /**
   * Error indicating that a comment body is empty.
   */
  case EmptyBody

  /**
   * Error indicating that a comment author ID is invalid.
   */
  case InvalidAuthorId

  /**
   * Error indicating that a comment ID is not positive.
   *
   * @param value the non-positive ID value that was provided
   */
  case IdIsNotPositive(value: Comment.Id)

  /**
   * Returns a human-readable message describing the validation error.
   *
   * @return the error message corresponding to this validation error case
   */
  override def message: String = this match
    case IdIsNotPositive(i) => s"Id must be positive, $i given"
    case EmptyBody          => "Comment body can not be empty"
    case InvalidAuthorId    => "Comment author id is invalid"
}
