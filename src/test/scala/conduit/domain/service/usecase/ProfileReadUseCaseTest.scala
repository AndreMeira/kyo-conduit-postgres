package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.user.GetProfileRequest
import conduit.domain.service.persistence.Persistence
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

object ProfileReadUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ProfileReadUseCase" should {
      "succeed in reading an existing profile" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          profile     <- database.transaction(fixtures.makeProfile(userId))
          request      = GetProfileRequest(
                           requester = User.Anonymous,
                           username = profile.name,
                         )
          response    <- ProfileReadUseCase(database, persistence).apply(request)
        yield assert(
          response.profile.username == profile.name &&
          !response.profile.following,
          "Expected profile response to match created profile",
        )
      }

      "fail to read a non-existent profile" in withDatabase { database =>
        for
          persistence <- makePersistence
          request      = GetProfileRequest(
                           requester = User.Anonymous,
                           username = "nonexistent-user",
                         )
          result      <- Abort.run(ProfileReadUseCase(database, persistence).apply(request))
          error        = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains("nonexistent-user"),
          "Expected error message to contain the username",
        )
      }
    }
}
