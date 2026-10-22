package conduit.infrastructure.postgres

import conduit.domain.model.{ User, UserProfile }
import conduit.domain.service.persistence.FollowerRepository
import conduit.infrastructure.codecs.database.SqlSchemaCodecs.given
import PostgresTransaction.Transactional
import kyo.*

/**
 * Postgres implementation of [[FollowerRepository]], over the `followers` table.
 */
class PostgresFollowerRepository extends FollowerRepository[PostgresTransaction]:

  /**
   * Whether the user follows the profile.
   *
   * @param followed the user and the profile
   * @return true when the relationship is recorded; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def exists(followed: UserProfile.Follower): Boolean < Effect =
    Transactional:
      Sql.from[UserProfile.Follower]("f", "followers")
        .where(row => row.f.followerId == followed.followerId && row.f.profileId == followed.profileId)
        .limit(1)
        .run
        .map(_.nonEmpty)

  /**
   * The profiles, among those given, that the user follows.
   *
   * @param followerId the user
   * @param followedIds the profiles to check
   * @return the followed ones; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def followedBy(followerId: User.Id, followedIds: List[UserProfile.Id]): List[UserProfile.Id] < Effect =
    Transactional:
      Sql.from[UserProfile.Follower]("f", "followers")
        .where(row => row.f.followerId == followerId && row.f.profileId.in(followedIds*))
        .select(_.f.profileId)
        .run
        .map(_.toList)

  /**
   * Records that the user follows the profile; a relationship already recorded is left as it is.
   *
   * @param followed the user and the profile
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the insert fails
   */
  override def add(followed: UserProfile.Follower): Unit < Effect =
    Transactional:
      Sql.insert[UserProfile.Follower]("followers")
        .values(followed)
        .onConflictDoNothing(_.followerId, _.profileId)
        .run
        .unit

  /**
   * Removes the relationship; one not recorded is ignored.
   *
   * @param followed the user and the profile
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the delete fails
   */
  override def delete(followed: UserProfile.Follower): Unit < Effect =
    Transactional:
      Sql.delete[UserProfile.Follower]("followers")
        .where(row => row.followerId == followed.followerId && row.profileId == followed.profileId)
        .run
        .unit
