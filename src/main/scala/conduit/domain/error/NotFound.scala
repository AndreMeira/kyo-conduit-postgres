package conduit.domain.error

import conduit.domain.model.User

/**
 * Enum representing not found errors in the application.
 *
 * This enum defines errors that occur when requested resources,
 * such as articles or user profiles, cannot be found in the system.
 */
enum NotFound extends ApplicationError.NotFoundError {

  /**
   * Error indicating that an article with the specified slug was not found.
   *
   * @param slug the slug of the article that was not found
   */
  case ArticleNotFound(slug: String)

  /**
   * Error indicating that a user profile with the specified username was not found.
   *
   * @param username the username of the profile that was not found
   */
  case ProfileNotFound(username: String)

  /**
   * Returns a human-readable message describing the not found error.
   *
   * @return the error message corresponding to this not found error case
   */
  override def message: String = this match {
    case ArticleNotFound(slug)     => s"Article with slug $slug not found"
    case ProfileNotFound(username) => s"Profile with username $username not found"
  }
}
