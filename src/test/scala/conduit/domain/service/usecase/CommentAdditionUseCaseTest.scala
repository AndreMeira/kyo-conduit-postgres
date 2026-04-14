package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.User
import conduit.domain.request.comment.AddCommentRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object CommentAdditionUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "CommentAdditionUseCase" should {
      "succeed in adding a comment to an existing article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          profile     <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          request      = AddCommentRequest(
                           requester = User.Authenticated(userId),
                           slug = article.slug,
                           payload = AddCommentRequest.Payload(
                             comment = AddCommentRequest.Data(
                               body = "This is a test comment."
                             )
                           ),
                         )
          response    <- CommentAdditionUseCase(database, persistence).apply(request)
        yield assert(
          response.comment.body == "This is a test comment." &&
          response.comment.author.username == profile.name,
          "Expected comment to be created with correct body and author",
        )
      }

      "fail with ArticleNotFound when adding a comment to a non-existent article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid
          request      = AddCommentRequest(
                           requester = User.Authenticated(userId),
                           slug = "nonexistent-article",
                           payload = AddCommentRequest.Payload(
                             comment = AddCommentRequest.Data(
                               body = "This is a test comment."
                             )
                           ),
                         )

          result <- Abort.run(CommentAdditionUseCase(database, persistence).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains("Article with slug nonexistent-article not found"),
          "Expected error message to indicate article not found",
        )
      }
    }
}
