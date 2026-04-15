package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.user.FollowUserRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.domain.types.*
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object ProfileFollowingUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ProfileFollowingUseCase" should {
      "succeed in following an existing profile" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          followerId  <- database.transaction(fixtures.makeUser)
          _           <- database.transaction(fixtures.makeProfile(followerId))
          targetId    <- database.transaction(fixtures.makeUser)
          targetProf  <- database.transaction(fixtures.makeProfile(targetId))
          request      = FollowUserRequest(
                           requester = User.Authenticated(followerId),
                           username = targetProf.name,
                         )
          response    <- ProfileFollowingUseCase(database, persistence).apply(request)
        yield assert(
          response.profile.username == targetProf.name &&
          response.profile.following,
          "Expected profile to be followed",
        )
      }

      "fail to follow a non-existent profile" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid.map(UserId(_))
          username     = "nonexistent_user"
          request      = FollowUserRequest(
                           requester = User.Authenticated(userId),
                           username = username,
                         )
          result      <- Abort.run(ProfileFollowingUseCase(database, persistence).apply(request))
          error        = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains(username),
          "Expected error message to contain the non-existent username",
        )
      }
    }
}
