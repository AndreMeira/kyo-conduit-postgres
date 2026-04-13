package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.article.GetArticleRequest
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.authentication.AuthenticationService.Config
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import scala.concurrent.duration.*

object ArticleReadUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  def makeAuthentication: AuthenticationService =
    AuthenticationService(Clock.live, Config("test-password-salt", "test-token-salt", 1.hour))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ArticleReadUseCase" should {
      "succeed in reading an existing article" in withDatabase { database =>
        for
          fixtures       <- makeFixtures
          persistence    <- makePersistence
          authentication  = makeAuthentication
          userId         <- database.transaction(fixtures.makeUser)
          profile        <- database.transaction(fixtures.makeProfile(userId))
          article        <- database.transaction(fixtures.makeArticle(userId))
          request         = GetArticleRequest(
                              requester = User.Anonymous,
                              slug = article.slug,
                            )
          response       <- ArticleReadUseCase(database, persistence, authentication).apply(request)
        yield assert(
          response.article.slug == article.slug &&
          response.article.title == article.title &&
          response.article.author.username == profile.name,
          "Expected article response to match created article data",
        )
      }

      "fail with ArticleNotFound when slug does not exist" in withDatabase { database =>
        for
          persistence    <- makePersistence
          authentication  = makeAuthentication
          request         = GetArticleRequest(
                              requester = User.Anonymous,
                              slug = "non-existent-slug",
                            )

          result <- Abort.run(ArticleReadUseCase(database, persistence, authentication).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains("non-existent-slug"),
          "Expected error message to indicate article not found",
        )
      }
    }
}
