package conduit.infrastructure.inmemory

import conduit.domain.model.UserProfile
import conduit.domain.service.persistence.FollowerRepository
import kyo.*

/**
 * In-memory implementation of the FollowerRepository for the Conduit application.
 *
 * This repository stores follower relationships in memory and provides operations for checking,
 * adding, and removing follow relationships between users. All operations are wrapped in InMemoryTransaction
 * to ensure consistent access to the shared followers state.
 */
class InMemoryFollowerRepository extends FollowerRepository[InMemoryTransaction] {
  /**
   * Checks if a follower relationship exists.
   *
   * Verifies whether a specific user follows another user by checking the followers
   * map in the current transaction state.
   *
   * @param followed the follower relationship to check
   * @return true if the follower relationship exists, false otherwise
   */
  override def exists(followed: UserProfile.FollowedBy): Boolean < Effect =
    InMemoryTransaction { state =>
      state.followers.get.map { followers =>
        followers.getOrElse(followed.followerId, Nil).contains(followed.profileId)
      }
    }

  /**
   * Adds a new follower relationship.
   *
   * Creates a new follow relationship by adding the follower ID to the list of followers
   * for the specified user.
   *
   * @param followed the follower relationship to add
   * @return Unit
   */
  override def add(followed: UserProfile.FollowedBy): Unit < Effect =
    InMemoryTransaction { state =>
      state.followers.updateAndGet { followers =>
        val currentFollowers = followers.getOrElse(followed.followerId, Nil)
        followers + (followed.followerId -> (currentFollowers :+ followed.profileId))
      }.unit
    }

  /**
   * Removes a follower relationship.
   *
   * Deletes an existing follow relationship by removing the follower ID from the list
   * of followers for the specified user.
   *
   * @param followed the follower relationship to remove
   * @return Unit
   */
  override def delete(followed: UserProfile.FollowedBy): Unit < Effect =
    InMemoryTransaction { state =>
      state.followers.updateAndGet { followers =>
        val currentFollowers = followers.getOrElse(followed.followerId, Nil)
        followers + (followed.followerId -> currentFollowers.filterNot(_ == followed.profileId))
      }.unit
    }
}

