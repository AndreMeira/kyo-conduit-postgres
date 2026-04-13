package conduit.infrastructure.postgres

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.UserProfile
import PostgresTestSupport.withDatabase
import conduit.infrastructure.TestFixtures
import kyo.*

object PostgresFollowerRepositorySpec extends KyoTestSuite:

  private val persistence = PostgresTestSupport.makePersistence
  private val fixtures    = TestFixtures(persistence)

  def specSuite: SuiteResult < (Async & Scope) =
    "PostgresFollowerRepository" should withDatabase { database =>

      "add and detect a follower relationship" in
        database.withMigration:
          database.transaction:
            for
              follower   <- fixtures.makeUser
              followeeId <- fixtures.makeUser
              followee   <- fixtures.makeProfile(followeeId)
              rel         = UserProfile.Follower(follower, followee.id)
              before     <- persistence.followers.exists(rel)
              _          <- persistence.followers.add(rel)
              after      <- persistence.followers.exists(rel)
            yield assert(!before, "should not exist before add") &
              assert(after, "should exist after add")

      "add is idempotent (ON CONFLICT DO NOTHING)" in
        database.withMigration:
          database.transaction:
            for
              follower   <- fixtures.makeUser
              followeeId <- fixtures.makeUser
              followee   <- fixtures.makeProfile(followeeId)
              rel         = UserProfile.Follower(follower, followee.id)
              _          <- persistence.followers.add(rel)
              _          <- persistence.followers.add(rel) // must not fail
              exists     <- persistence.followers.exists(rel)
            yield assert(exists, "should still exist after double-add")

      "delete a follower relationship" in
        database.withMigration:
          database.transaction:
            for
              follower   <- fixtures.makeUser
              followeeId <- fixtures.makeUser
              followee   <- fixtures.makeProfile(followeeId)
              rel         = UserProfile.Follower(follower, followee.id)
              _          <- persistence.followers.add(rel)
              _          <- persistence.followers.delete(rel)
              exists     <- persistence.followers.exists(rel)
            yield assert(!exists, "should not exist after delete")

      "delete is a no-op for a missing relationship" in
        database.withMigration:
          database.transaction:
            for
              follower   <- fixtures.makeUser
              followeeId <- fixtures.makeUser
              followee   <- fixtures.makeProfile(followeeId)
              rel         = UserProfile.Follower(follower, followee.id)
              _          <- persistence.followers.delete(rel) // must not fail
              exists     <- persistence.followers.exists(rel)
            yield assert(!exists, "should not exist")

      "followedBy returns only actually-followed ids" in
        database.withMigration:
          database.transaction:
            for
              follower <- fixtures.makeUser
              u1Id     <- fixtures.makeUser
              u2Id     <- fixtures.makeUser
              u3Id     <- fixtures.makeUser
              p1       <- fixtures.makeProfile(u1Id)
              p2       <- fixtures.makeProfile(u2Id)
              p3       <- fixtures.makeProfile(u3Id)
              _        <- persistence.followers.add(UserProfile.Follower(follower, p1.id))
              _        <- persistence.followers.add(UserProfile.Follower(follower, p3.id))
              result   <- persistence.followers.followedBy(follower, List(p1.id, p2.id, p3.id))
            yield assert(result.toSet == Set(p1.id, p3.id), s"Expected {p1,p3}, got $result")

      "followedBy returns empty for empty input" in
        database.withMigration:
          database.transaction:
            for
              follower <- fixtures.makeUser
              result   <- persistence.followers.followedBy(follower, List.empty)
            yield assert(result.isEmpty, s"Expected empty, got $result")
    }
