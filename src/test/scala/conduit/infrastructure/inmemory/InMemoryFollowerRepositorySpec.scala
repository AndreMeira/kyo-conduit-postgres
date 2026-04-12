package conduit.infrastructure.inmemory

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.UserProfile
import conduit.domain.service.persistence.Persistence
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.TestFixtures
import kyo.*

object InMemoryFollowerRepositorySpec extends KyoTestSuite:

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  def specSuite: SuiteResult < (Async & Scope) =

    "InMemoryFollowerRepository" should {
      "add and detect a follower relationship" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            follower    <- fixtures.makeUser
            followeeId  <- fixtures.makeUser
            followee    <- fixtures.makeProfile(followeeId)
            rel          = UserProfile.FollowedBy(follower, followee.id)
            before      <- persistence.followers.exists(rel)
            _           <- persistence.followers.add(rel)
            after       <- persistence.followers.exists(rel)
          yield assert(!before, "should not exist before add") &
            assert(after, "should exist after add")
      }

      "add is idempotent" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            follower    <- fixtures.makeUser
            followeeId  <- fixtures.makeUser
            followee    <- fixtures.makeProfile(followeeId)
            rel          = UserProfile.FollowedBy(follower, followee.id)
            _           <- persistence.followers.add(rel)
            _           <- persistence.followers.add(rel) // must not fail
            exists      <- persistence.followers.exists(rel)
          yield assert(exists, "should still exist after double-add")
      }

      "delete a follower relationship" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            follower    <- fixtures.makeUser
            followeeId  <- fixtures.makeUser
            followee    <- fixtures.makeProfile(followeeId)
            rel          = UserProfile.FollowedBy(follower, followee.id)
            _           <- persistence.followers.add(rel)
            _           <- persistence.followers.delete(rel)
            exists      <- persistence.followers.exists(rel)
          yield assert(!exists, "should not exist after delete")
      }

      "delete is a no-op for a missing relationship" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            follower    <- fixtures.makeUser
            followeeId  <- fixtures.makeUser
            followee    <- fixtures.makeProfile(followeeId)
            rel          = UserProfile.FollowedBy(follower, followee.id)
            _           <- persistence.followers.delete(rel) // must not fail
            exists      <- persistence.followers.exists(rel)
          yield assert(!exists, "should not exist")
      }

      "followedBy returns only actually-followed ids" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            follower    <- fixtures.makeUser
            u1Id        <- fixtures.makeUser
            u2Id        <- fixtures.makeUser
            u3Id        <- fixtures.makeUser
            p1          <- fixtures.makeProfile(u1Id)
            p2          <- fixtures.makeProfile(u2Id)
            p3          <- fixtures.makeProfile(u3Id)
            _           <- persistence.followers.add(UserProfile.FollowedBy(follower, p1.id))
            _           <- persistence.followers.add(UserProfile.FollowedBy(follower, p3.id))
            result      <- persistence.followers.followedBy(follower, List(p1.id, p2.id, p3.id))
          yield assert(result.toSet == Set(p1.id, p3.id), s"Expected {p1,p3}, got $result")
      }

      "followedBy returns empty for empty input" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            follower    <- fixtures.makeUser
            result      <- persistence.followers.followedBy(follower, List.empty)
          yield assert(result.isEmpty, s"Expected empty, got $result")
      }
    }
