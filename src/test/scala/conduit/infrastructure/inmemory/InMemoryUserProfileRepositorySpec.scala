package conduit.infrastructure.inmemory

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.TestFixtures
import kyo.*

object InMemoryUserProfileRepositorySpec extends KyoTestSuite:

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  def specSuite: SuiteResult < (Async & Scope) =

    "InMemoryUserProfileRepository" should {
      "save and find by profile id" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            profile     <- fixtures.makeProfile(userId)
            found       <- persistence.users.find(profile.id)
          yield assert(found == Maybe.Present(profile), s"Expected $profile but got $found")
      }

      "save and find by user id" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            profile     <- fixtures.makeProfile(userId)
            found       <- persistence.users.findByUser(userId)
          yield assert(found == Maybe.Present(profile), s"Expected $profile but got $found")
      }

      "save and find by username" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            username     = s"name-${java.util.UUID.randomUUID()}"
            profile     <- fixtures.makeProfile(userId, Some(username))
            found       <- persistence.users.findByUsername(username)
          yield assert(found == Maybe.Present(profile), s"Expected $profile but got $found")
      }

      "return Emtpy for unknown id" in withDatabase { database =>
        database.transaction:
          for
            persistence <- makePersistence
            unknownId   <- IdGeneratorService.uuid
            found       <- persistence.users.find(unknownId)
            exists      <- persistence.users.exists(unknownId)
          yield assert(found == Maybe.Absent, s"Expected Emtpy but got $found") &
            assert(!exists, "expected exists(unknown)=false")
      }

      "report existence of saved profile by id and by username" in withDatabase { database =>
        database.transaction:
          for
            fixtures     <- makeFixtures
            persistence  <- makePersistence
            userId       <- fixtures.makeUser
            profile      <- fixtures.makeProfile(userId)
            existsById   <- persistence.users.exists(profile.id)
            existsByName <- persistence.users.exists(profile.name)
          yield assert(existsById, s"expected exists(id=${profile.id})=true") &
            assert(existsByName, s"expected exists(name=${profile.name})=true")
      }

      "findByUsers returns profiles for all given user ids" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            u1          <- fixtures.makeUser
            p1          <- fixtures.makeProfile(u1)
            u2          <- fixtures.makeUser
            p2          <- fixtures.makeProfile(u2)
            u3          <- fixtures.makeUser
            p3          <- fixtures.makeProfile(u3)
            found       <- persistence.users.findByUsers(List(u1, u2, u3))
          yield assert(found.toSet == Set(p1, p2, p3), s"Expected {p1,p2,p3}, got $found")
      }

      "findByUsers returns empty for empty input" in withDatabase { database =>
        database.transaction:
          for
            persistence <- makePersistence
            found       <- persistence.users.findByUsers(List.empty)
          yield assert(found.isEmpty, s"Expected empty, got $found")
      }

      "update profile" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            profile     <- fixtures.makeProfile(userId)
            ts          <- fixtures.now
            updated      = profile.copy(biography = Maybe.Present("an updated bio"), updatedAt = ts)
            _           <- persistence.users.update(updated)
            found       <- persistence.users.find(profile.id)
          yield assert(found == Maybe.Present(updated), s"Expected $updated but got $found")
      }

      "findByArticle returns author profile" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            profile     <- fixtures.makeProfile(userId)
            article     <- fixtures.makeArticle(userId)
            found       <- persistence.users.findByArticle(article.id)
          yield assert(found == Maybe.Present(profile), s"Expected $profile but got $found")
      }

      "findByArticles returns a map of article id to author profile" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            u1          <- fixtures.makeUser
            p1          <- fixtures.makeProfile(u1)
            a1          <- fixtures.makeArticle(u1, "First")
            u2          <- fixtures.makeUser
            p2          <- fixtures.makeProfile(u2)
            a2          <- fixtures.makeArticle(u2, "Second")
            found       <- persistence.users.findByArticles(Set(a1.id, a2.id))
          yield assert(
            found == Map(a1.id -> p1, a2.id -> p2),
            s"Expected {a1->p1, a2->p2}, got $found",
          )
      }

      "findByArticles returns empty for empty input" in withDatabase { database =>
        database.transaction:
          for
            persistence <- makePersistence
            found       <- persistence.users.findByArticles(Set.empty)
          yield assert(found.isEmpty, s"Expected empty, got $found")
      }
    }
