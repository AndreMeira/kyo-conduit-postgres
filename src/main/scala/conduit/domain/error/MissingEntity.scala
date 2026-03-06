package conduit.domain.error

import ApplicationError.InconsistentState
import conduit.domain.model.{ Article, User, UserProfile }

import java.util.UUID

/**
 * Enum representing inconsistent state errors when required entities are missing.
 *
 * This enum defines errors that occur when the application encounters an inconsistent
 * state where expected entities (such as user profiles or articles) cannot be found.
 * These errors typically indicate a bug or data corruption issue.
 */
enum MissingEntity extends InconsistentState:
  /**
   * Error indicating that a user profile is missing for a given user ID.
   *
   * This error occurs when a user ID exists but its associated profile cannot be found,
   * suggesting data inconsistency.
   *
   * @param userId the ID of the user whose profile is missing
   */
  case UserProfileMissing(userId: User.Id | UserProfile.Id)

  /**
   * Error indicating that an article is missing for a given article ID.
   *
   * This error occurs when an article ID exists but the article itself cannot be found,
   * suggesting data inconsistency.
   *
   * @param articleId the ID of the missing article
   */
  case ArticleMissing(articleId: Article.Id)

  /**
   * Error indicating that credentials are missing for a given user ID.
   *
   * This error occurs when a user ID exists but its associated credentials cannot be found,
   * suggesting data inconsistency.
   *
   * @param userId the ID of the user whose credentials are missing
   */
  case CredentialsMissing(userId: User.Id | UserProfile.Id)

  /**
   * Returns a human-readable message describing the missing entity error.
   *
   * @return the error message corresponding to this missing entity error case
   */
  override def message: String = this match
    case UserProfileMissing(id) => s"User profile of user $id is missing"
    case ArticleMissing(id)     => s"Article with id $id is missing"
    case CredentialsMissing(id) => s"Credentials of user $id are missing"
