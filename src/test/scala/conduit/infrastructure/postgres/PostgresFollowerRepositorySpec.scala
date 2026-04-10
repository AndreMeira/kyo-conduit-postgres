package conduit.infrastructure.postgres

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.UserProfile
import PostgresTestSupport.withDatabase
import kyo.*

object PostgresFollowerRepositorySpec extends KyoTestSuite:

  private val persistence = PostgresTestSupport.makePersistence
  private val fixtures    = TestFixtures(persistence)

  def specSuite: SuiteResult < (Async & Scope) =
    "PostgresFollowerRepository" should withDatabase { database =>

      "add and detect a follower relationship" in
        database.withCleanDatabase:
          database.transaction:
            for
              follower <- fixtures.makeUser
              followee <- fixtures.makeUser
              rel       = UserProfile.FollowedBy(follower, followee)
              before   <- persistence.followers.exists(rel)
              _        <- persistence.followers.add(rel)
              after    <- persistence.followers.exists(rel)
            yield assert(!before, "should not exist before add") &
              assert(after, "should exist after add")

      "add is idempotent (ON CONFLICT DO NOTHING)" in
        database.withCleanDatabase:
          database.transaction:
            for
              follower <- fixtures.makeUser
              followee <- fixtures.makeUser
              rel       = UserProfile.FollowedBy(follower, followee)
              _        <- persistence.followers.add(rel)
              _        <- persistence.followers.add(rel) // must not fail
              exists   <- persistence.followers.exists(rel)
            yield assert(exists, "should still exist after double-add")

      "delete a follower relationship" in
        database.withCleanDatabase:
          database.transaction:
            for
              follower <- fixtures.makeUser
              followee <- fixtures.makeUser
              rel       = UserProfile.FollowedBy(follower, followee)
              _        <- persistence.followers.add(rel)
              _        <- persistence.followers.delete(rel)
              exists   <- persistence.followers.exists(rel)
            yield assert(!exists, "should not exist after delete")

      "delete is a no-op for a missing relationship" in
        database.withCleanDatabase:
          database.transaction:
            for
              follower <- fixtures.makeUser
              followee <- fixtures.makeUser
              rel       = UserProfile.FollowedBy(follower, followee)
              _        <- persistence.followers.delete(rel) // must not fail
              exists   <- persistence.followers.exists(rel)
            yield assert(!exists, "should not exist")

      "followedBy returns only actually-followed ids" in
        database.withCleanDatabase:
          database.transaction:
            for
              follower <- fixtures.makeUser
              u1       <- fixtures.makeUser
              u2       <- fixtures.makeUser
              u3       <- fixtures.makeUser
              _        <- persistence.followers.add(UserProfile.FollowedBy(follower, u1))
              _        <- persistence.followers.add(UserProfile.FollowedBy(follower, u3))
              result   <- persistence.followers.followedBy(follower, List(u1, u2, u3))
            yield assert(result.toSet == Set(u1, u3), s"Expected {u1,u3}, got $result")

      "followedBy returns empty for empty input" in
        database.withCleanDatabase:
          database.transaction:
            for
              follower <- fixtures.makeUser
              result   <- persistence.followers.followedBy(follower, List.empty)
            yield assert(result.isEmpty, s"Expected empty, got $result")
    }
