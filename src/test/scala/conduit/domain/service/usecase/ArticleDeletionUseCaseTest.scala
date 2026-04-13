package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.article.DeleteArticleRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object ArticleDeletionUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ArticleDeletionUseCase" should {
      "succeed in deleting an existing article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          _           <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          request      = DeleteArticleRequest(
                           requester = User.Authenticated(userId),
                           slug = article.slug,
                         )
          response    <- ArticleDeletionUseCase(database, persistence).apply(request)
          found       <- database.transaction(persistence.articles.find(article.id))
        yield ok &
          assert(response.article == article.slug, "Expected deleted article slug to match") &
          assert(found.isEmpty, "Expected article to be deleted and not found in repository")
      }

      "fail with ArticleNotFound when deleting a non-existent slug" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid
          request      = DeleteArticleRequest(
                           requester = User.Authenticated(userId),
                           slug = "non-existent-slug",
                         )

          result <- Abort.run(ArticleDeletionUseCase(database, persistence).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains("Article with slug non-existent-slug not found"),
          "Expected error message to indicate article not found",
        )
      }
    }
}
