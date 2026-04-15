package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.article.AddFavoriteArticleRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.domain.types.*
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object ArticleFavoriteUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ArticleFavoriteUseCase" should {
      "succeed in favoriting an existing article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          profile     <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          request      = AddFavoriteArticleRequest(
                           requester = User.Authenticated(userId),
                           slug = article.slug,
                         )
          response    <- ArticleFavoriteUseCase(database, persistence).apply(request)
        yield assert(
          response.article.slug == article.slug &&
          response.article.favorited &&
          response.article.author.username == profile.name,
          "Expected article to be favorited with correct data",
        )
      }

      "fail to favorite an article when the article does not exist" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid.map(UserId(_))
          slug         = "non-existent-article"
          request      = AddFavoriteArticleRequest(
                           requester = User.Authenticated(userId),
                           slug = slug,
                         )

          result <- Abort.run(ArticleFavoriteUseCase(database, persistence).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains(s"Article with slug $slug not found"),
          "Expected error message to indicate article not found",
        )
      }
    }
}
