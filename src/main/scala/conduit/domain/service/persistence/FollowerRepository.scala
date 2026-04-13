package conduit.domain.service.persistence

import conduit.domain.error.ApplicationError
import conduit.domain.model.UserProfile
import conduit.domain.service.persistence.Database.Transaction
import kyo.*

/**
 * Repository trait for managing follower relationships between users.
 *
 * This trait defines the contract for storing and managing user follower relationships,
 * allowing different implementations (in-memory, database, etc.) to provide persistent
 * storage for follow/unfollow operations in the Conduit application.
 *
 * Followers represent a many-to-many relationship where one user can follow multiple
 * other users, and one user can be followed by multiple followers.
 *
 * @tparam Tx the transaction type used for database operations
 */
trait FollowerRepository[Tx <: Transaction] {
  type Effect = Async & Env[Tx] & Abort[ApplicationError]

  /**
   * Checks if a follower relationship exists.
   *
   * Verifies whether a specific user follows another user based on the
   * provided follower relationship information.
   *
   * @param followed the follower relationship to check
   * @return true if the follower relationship exists, false otherwise
   */
  def exists(followed: UserProfile.Follower): Boolean < Effect

  /**
   * Finds follower relationships for a user across multiple followed users.
   *
   * Retrieves all follower relationships where a specific user is following
   * any of the users in the provided list of followed user IDs.
   *
   * @param followerId the ID of the follower user
   * @param followedIds the list of followed user IDs to check
   * @return List of follower relationships found
   */
  def followedBy(followerId: UserProfile.Id, followedIds: List[UserProfile.Id]): List[UserProfile.Id] < Effect

  /**
   * Adds a new follower relationship.
   *
   * Creates a new follow relationship between two users. After this operation,
   * one user will be following another user.
   *
   * @param followed the follower relationship to add
   * @return Unit on successful addition
   */
  def add(followed: UserProfile.Follower): Unit < Effect

  /**
   * Removes a follower relationship.
   *
   * Deletes an existing follow relationship between two users. After this operation,
   * one user will no longer follow another user.
   *
   * @param followed the follower relationship to remove
   * @return Unit on successful deletion
   */
  def delete(followed: UserProfile.Follower): Unit < Effect
}
