package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Article, User }
import conduit.domain.request.article.RemoveFavoriteArticleRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object ArticleUnfavoriteUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ArticleUnfavoriteUseCase" should {
      "succeed in unfavoriting an existing article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          profile     <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          _           <- database.transaction(persistence.favorites.add(Article.FavoriteBy(userId, article.id)))
          exists      <- database.transaction(persistence.favorites.exists(Article.FavoriteBy(userId, article.id)))
          request      = RemoveFavoriteArticleRequest(
                           requester = User.Authenticated(userId),
                           slug = article.slug,
                         )
          response    <- ArticleUnfavoriteUseCase(database, persistence).apply(request)
          found       <- database.transaction(persistence.favorites.exists(Article.FavoriteBy(userId, article.id)))
        yield assert(
          response.article.slug == article.slug &&
          !response.article.favorited &&
          response.article.author.username == profile.name,
          "Expected article to be unfavorited with correct data",
        ) &
          assert(exists, "Expected favorite to exist before unfavoriting") &
          assert(!found, "Expected favorite to be removed after unfavoriting")
      }

      "fail with ArticleNotFound when unfavoriting a non-existent slug" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid
          slug         = "non-existent-slug"
          request      = RemoveFavoriteArticleRequest(
                           requester = User.Authenticated(userId),
                           slug = slug,
                         )

          result <- Abort.run(ArticleUnfavoriteUseCase(database, persistence).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains(s"Article with slug $slug not found"),
          "Expected error message to indicate article not found",
        )
      }
    }
}
