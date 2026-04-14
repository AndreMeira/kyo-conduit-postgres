package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.Patchable
import conduit.domain.request.user.UpdateUserRequest
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.authentication.AuthenticationService.Config
import conduit.domain.service.persistence.{IdGeneratorService, Persistence}
import conduit.domain.service.validation.StateValidationService
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{InMemoryTestSupport, InMemoryTransaction}
import kyo.*

import java.util.UUID
import scala.concurrent.duration.Duration

object UserUpdateUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "UserUpdateUseCase" should {
      "succeed in updating an existing user's bio" in withDatabase { database =>
        for
          fixtures         <- makeFixtures
          persistence      <- makePersistence
          config            = Config(passwordSalt = "test-salt", tokenSalt = "test-token-salt", tokenTtl = Duration("1h"))
          authentication    = AuthenticationService(Clock.live, config)
          stateValidation   = StateValidationService[InMemoryTransaction](persistence)
          userId           <- database.transaction(fixtures.makeUser)
          _                <- database.transaction(fixtures.makeProfile(userId))
          request           = UpdateUserRequest(
                                requester = User.Authenticated(userId),
                                payload = UpdateUserRequest.Payload(
                                  user = UpdateUserRequest.Data(
                                    email = None,
                                    username = None,
                                    password = None,
                                    bio = Patchable.Present("updated bio"),
                                    image = Patchable.Emtpy,
                                  )
                                ),
                              )
          response         <- UserUpdateUseCase(database, persistence, authentication, stateValidation).apply(request)
        yield assert(
          response.user.bio.contains("updated bio") &&
          response.user.token.nonEmpty,
          "Expected user bio to be updated",
        )
      }

      "fail to update user when profile doesn't exist" in withDatabase { database =>
        for
          fixtures         <- makeFixtures
          persistence      <- makePersistence
          userId           <- IdGeneratorService.uuid
          clock             = Clock.live
          config            = Config(passwordSalt = "test-salt", tokenSalt = "test-token-salt", tokenTtl = Duration("1h"))
          authentication    = AuthenticationService(clock, config)
          stateValidation   = StateValidationService[InMemoryTransaction](persistence)
          request           = UpdateUserRequest(
                                requester = User.Authenticated(userId),
                                payload = UpdateUserRequest.Payload(
                                  user = UpdateUserRequest.Data(
                                    email = None,
                                    username = None,
                                    password = None,
                                    bio = Patchable.Present("new bio"),
                                    image = Patchable.Emtpy,
                                  )
                                ),
                              )

          result <- Abort.run(UserUpdateUseCase(database, persistence, authentication, stateValidation).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains(s"User profile of user $userId is missing"),
          "Expected error message to indicate user profile not found",
        )
      }
    }
}
