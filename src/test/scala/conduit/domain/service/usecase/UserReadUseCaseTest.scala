package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.user.GetUserRequest
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.authentication.AuthenticationService.Config
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.domain.types.*
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID
import scala.concurrent.duration.*

object UserReadUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "UserReadUseCase" should {
      "succeed in reading an existing user" in withDatabase { database =>
        for
          fixtures      <- makeFixtures
          persistence   <- makePersistence
          config         = Config(passwordSalt = "test-salt", tokenSalt = "test-token-salt", tokenTtl = 1.hour)
          authentication = AuthenticationService(Clock.live, config)
          userId        <- database.transaction(fixtures.makeUser)
          profile       <- database.transaction(fixtures.makeProfile(userId))
          request        = GetUserRequest(User.Authenticated(userId))
          response      <- UserReadUseCase(database, persistence, authentication).apply(request)
        yield assert(
          response.user.username == profile.name &&
          response.user.token.nonEmpty,
          "Expected user response to contain correct username and token",
        )
      }

      "fail to read user when profile doesn't exist" in withDatabase { database =>
        for
          fixtures      <- makeFixtures
          persistence   <- makePersistence
          userId        <- IdGeneratorService.uuid.map(UserId(_))
          clock          = Clock.live
          config         = Config(passwordSalt = "test-salt", tokenSalt = "test-token-salt", tokenTtl = 1.hour)
          authentication = AuthenticationService(clock, config)
          request        = GetUserRequest(User.Authenticated(userId))
          result        <- Abort.run(UserReadUseCase(database, persistence, authentication).apply(request))
          error          = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains(s"User profile of user $userId is missing"),
          "Expected error message to indicate user profile not found",
        )
      }
    }
}
