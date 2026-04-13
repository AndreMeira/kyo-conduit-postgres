package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.User
import conduit.domain.request.user.RegistrationRequest
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.Persistence
import conduit.domain.service.validation.StateValidationService
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import scala.concurrent.duration.Duration

object UserRegistrationUseCaseTest extends KyoTestSuite {

  private val authConfig = AuthenticationService.Config(
    passwordSalt = "test-password-salt",
    tokenSalt = "test-token-salt",
    tokenTtl = Duration("1 hour"),
  )

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  override def specSuite: SuiteResult < (Async & Scope) =
    "UserRegistrationUseCase" should {
      "successfully register a new user" in withDatabase { database =>
        for
          persistence    <- makePersistence
          authentication  = AuthenticationService(Clock.live, authConfig)
          stateValidation = StateValidationService[InMemoryTransaction](persistence)
          useCase         = UserRegistrationUseCase(database, persistence, authentication, stateValidation)
          request         = RegistrationRequest(
                              requester = User.Anonymous,
                              payload = RegistrationRequest.Payload(
                                user = RegistrationRequest.Data(
                                  username = "testuser",
                                  email = "test@example.com",
                                  password = "password123",
                                )
                              ),
                            )
          result <- Abort.run(useCase.apply(request))
          response = result.toEither.toOption.get
        yield
          assert(response.user.email == "test@example.com", "Expected email to match") &
          assert(response.user.username == "testuser", "Expected username to match")
      }
    }
}
