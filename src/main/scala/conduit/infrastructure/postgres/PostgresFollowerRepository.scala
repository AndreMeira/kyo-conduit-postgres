package conduit.infrastructure.postgres

import com.augustnagro.magnum.*
import conduit.domain.model.UserProfile
import conduit.domain.service.persistence.FollowerRepository
import conduit.infrastructure.codecs.database.DatabaseCodecs.given
import conduit.infrastructure.postgres.PostgresTransaction.Transactional
import kyo.*

import java.util.UUID

class PostgresFollowerRepository extends FollowerRepository[PostgresTransaction]:

  /**
   * Checks if a follower relationship exists.
   *
   * follower_id references users(id), followee_id references profiles(id).
   * Follower.profileId stores the profile ID of the followed user.
   *
   * @param followed the follower relationship to check
   * @return true if the follower relationship exists, false otherwise
   */
  override def exists(followed: UserProfile.Follower): Boolean < Effect =
    Transactional:
      sql"""SELECT EXISTS(
              SELECT 1 FROM followers
              WHERE follower_id = ${followed.followerId}
                AND followee_id = ${followed.profileId}
            )"""
        .query[Boolean]
        .run()
        .headOption
        .contains(true)

  /**
   * Finds which of the given followee IDs are followed by the specified follower.
   *
   * @param followerId the ID of the follower user
   * @param followedIds the list of followee IDs to check
   * @return the subset of followedIds that the follower is following
   */
  override def followedBy(followerId: UserProfile.Id, followedIds: List[UserProfile.Id]): List[UserProfile.Id] < Effect =
    if followedIds.isEmpty then List.empty
    else
      Transactional:
        val ids: List[UUID] = followedIds
        sql"""SELECT followee_id FROM followers
              WHERE follower_id = $followerId
                AND followee_id = ANY($ids)"""
          .query[UserProfile.Id]
          .run()
          .toList

  /**
   * Adds a new follower relationship.
   *
   * Uses ON CONFLICT DO NOTHING to make the operation idempotent.
   *
   * @param followed the follower relationship to add
   * @return Unit on successful addition
   */
  override def add(followed: UserProfile.Follower): Unit < Effect =
    Transactional {
      sql"""INSERT INTO followers (follower_id, followee_id)
            VALUES (${followed.followerId}, ${followed.profileId})
            ON CONFLICT (follower_id, followee_id) DO NOTHING"""
        .update
        .run()
    }.unit

  /**
   * Removes a follower relationship.
   *
   * @param followed the follower relationship to remove
   * @return Unit on successful deletion
   */
  override def delete(followed: UserProfile.Follower): Unit < Effect =
    Transactional {
      sql"""DELETE FROM followers
            WHERE follower_id = ${followed.followerId}
              AND followee_id = ${followed.profileId}"""
        .update
        .run()
    }.unit
