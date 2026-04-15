package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.article.CreateArticleRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.domain.types.*
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object ArticleCreationUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  /**
   * Defines the test suite to be executed.
   *
   * @return A suite result containing the results of all tests in the suite.
   */
  override def specSuite: SuiteResult < (Async & Scope) =
    "ArticleCreationUseCase" should {
      "succeed in creating an article with valid input" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          profile     <- database.transaction(fixtures.makeProfile(userId))
          request      = CreateArticleRequest(
                           requester = User.Authenticated(userId),
                           payload = CreateArticleRequest.Payload(
                             article = CreateArticleRequest.Data(
                               title = "Test Article",
                               description = "A test article for unit testing",
                               body = "This is the body of the test article.",
                               tagList = Some(List("test", "article")),
                             )
                           ),
                         )
          response    <- ArticleCreationUseCase(database, persistence).apply(request)
          found       <- database.transaction(persistence.articles.findBySlug(response.article.slug))
        yield assert(
          response.article.title == "Test Article" &&
          response.article.description == "A test article for unit testing" &&
          response.article.body == "This is the body of the test article." &&
          response.article.tagList == List("test", "article") &&
          response.article.author.username == profile.name,
          "Expected article to be created with correct data and author",
        ) & assert(
          found.nonEmpty,
          "Expected created article to be found in repository",
        )
      }

      "fail to create an article when the user doesn't exist" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid.map(UserId(_))
          request      = CreateArticleRequest(
                           requester = User.Authenticated(userId),
                           payload = CreateArticleRequest.Payload(
                             article = CreateArticleRequest.Data(
                               title = "Test Article",
                               description = "A test article for unit testing",
                               body = "This is the body of the test article.",
                               tagList = Some(List("test", "article")),
                             )
                           ),
                         )

          result <- Abort.run(ArticleCreationUseCase(database, persistence).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains(s"User profile of user $userId is missing"),
          "Expected error message to indicate user not found",
        )
      }
    }
}
