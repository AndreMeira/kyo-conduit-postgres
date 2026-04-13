package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.article.UpdateArticleRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object ArticleUpdateUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ArticleUpdateUseCase" should {
      "succeed in updating an existing article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          profile     <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          request      = UpdateArticleRequest(
                           requester = User.Authenticated(userId),
                           slug = article.slug,
                           payload = UpdateArticleRequest.Payload(
                             article = UpdateArticleRequest.Data(
                               title = Some("Updated Title"),
                               description = Some("Updated description"),
                               body = Some("Updated body content"),
                             )
                           ),
                         )
          response    <- ArticleUpdateUseCase(database, persistence).apply(request)
          found       <- database.transaction(persistence.articles.findBySlug(response.article.slug))
        yield ok &
          assert(
            response.article.author.username == profile.name,
            "Expected updated article response to contain the correct author",
          ) & assert(
            response.article.title == "Updated Title",
            "Expected article to be updated with new title",
          ) & assert(
            response.article.description == "Updated description",
            "Expected article to be updated with new description",
          ) & assert(
            response.article.body.contains("Updated body content"),
            "Expected article to be updated with new body content",
          ) & assert(
            found.exists(_.title == "Updated Title"),
            "Expected article to be updated in the database with new title",
          ) & assert(
            found.exists(_.description == "Updated description"),
            "Expected article to be updated in the database with new description",
          ) & assert(
            found.exists(_.body.contains("Updated body content")),
            "Expected article to be updated in the database with new body content",
          )
      }

      "fail with ArticleNotFound when updating a non-existent slug" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid
          slug         = "non-existent-slug"
          request      = UpdateArticleRequest(
                           requester = User.Authenticated(userId),
                           slug = slug,
                           payload = UpdateArticleRequest.Payload(
                             article = UpdateArticleRequest.Data(
                               title = Some("Updated Title"),
                               description = Some("Updated description"),
                               body = Some("Updated body content"),
                             )
                           ),
                         )

          result <- Abort.run(ArticleUpdateUseCase(database, persistence).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains(s"Article with slug $slug not found"),
          "Expected error message to indicate article not found",
        )
      }
    }
}
