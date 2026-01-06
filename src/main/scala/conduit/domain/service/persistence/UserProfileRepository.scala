package conduit.domain.service.persistence

import conduit.domain.error.ApplicationError
import conduit.domain.model.*
import conduit.domain.service.persistence.Database.Transaction
import kyo.*

/**
 * Repository trait for managing user profile persistence operations.
 *
 * This trait defines the contract for saving, updating, checking existence,
 * and retrieving user profiles, allowing different implementations (in-memory,
 * database, etc.) to provide persistent storage for user profiles in the
 * Conduit application.
 *
 * @tparam Tx the transaction type used for database operations
 */
trait UserProfileRepository[Tx <: Transaction] {
  type Effect = Async & Abort[ApplicationError] & Env[Tx]

  /**
   * Saves a new user profile to the repository.
   *
   * @param profile the user profile to save
   * @return Unit on successful save
   */
  def save(profile: UserProfile): Unit < Effect

  /**
   * Updates an existing user profile in the repository.
   *
   * @param profile the user profile with updated data
   * @return Unit on successful update
   */
  def update(profile: UserProfile): Unit < Effect

  /**
   * Checks if a user profile with the given ID exists in the repository.
   *
   * @param id the user profile ID to check
   * @return true if the user profile exists, false otherwise
   */
  def exists(id: UserProfile.Id): Boolean < Effect

  /**
   * Finds a user profile by its unique ID.
   *
   * @param id the user profile ID to search for
   * @return a Maybe containing the user profile if found, or None if not found
   */
  def find(id: UserProfile.Id): Maybe[UserProfile] < Effect

  /**
   * Finds a user profile by the associated user ID.
   *
   * @param id the user ID to search for
   * @return a Maybe containing the user profile if found, or None otherwise
   */
  def findByUser(id: User.Id): Maybe[UserProfile] < Effect

  /**
   * Finds a user profile by the associated article ID.
   *
   * @param id the article ID to search for
   * @return a Maybe containing the user profile if found, or None otherwise
   */
  def findByArticle(id: Article.Id): Maybe[UserProfile] < Effect
}
