package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Credentials, User }
import conduit.domain.request.user.AuthenticateRequest
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.domain.types.*
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID
import scala.concurrent.duration.*

object UserAuthenticationUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  private val testConfig = AuthenticationService.Config(
    passwordSalt = "test-password-salt",
    tokenSalt = "test-token-salt",
    tokenTtl = 1.hour,
  )

  private val authentication = AuthenticationService(Clock.live, testConfig)

  override def specSuite: SuiteResult < (Async & Scope) =
    "UserAuthenticationUseCase" should {
      "succeed in authenticating a user with valid credentials" in withDatabase { database =>
        for
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid.map(UserId(_))
          hashed      <- authentication.hash(Credentials.Clear(Email("test@example.com"), Password("password123")))
          _           <- database.transaction(persistence.credentials.save(userId, hashed))
          _           <- database.transaction(TestFixtures(persistence).makeProfile(userId))
          request      = AuthenticateRequest(
                           requester = User.Anonymous,
                           payload = AuthenticateRequest.Payload(
                             user = AuthenticateRequest.Data(
                               email = "test@example.com",
                               password = "password123",
                             )
                           ),
                         )
          response    <- UserAuthenticationUseCase(database, persistence, authentication).apply(request)
        yield assert(
          response.user.email == "test@example.com" &&
          response.user.token.nonEmpty,
          "Expected authentication to succeed with valid token",
        )
      }

      "fail with invalid credentials when user does not exist" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          request      = AuthenticateRequest(
                           requester = User.Anonymous,
                           payload = AuthenticateRequest.Payload(
                             user = AuthenticateRequest.Data(
                               email = "nonexistent@example.com",
                               password = "password123",
                             )
                           ),
                         )

          result <- Abort.run(UserAuthenticationUseCase(database, persistence, authentication).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message == "Invalid email/password",
          "Expected error message to indicate invalid credentials",
        )
      }
    }
}
