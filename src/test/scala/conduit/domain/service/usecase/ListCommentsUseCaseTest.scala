package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Comment, User }
import conduit.domain.request.comment.ListCommentsRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.domain.types.*
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object ListCommentsUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ListCommentsUseCase" should {
      "return comments for an existing article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          _           <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          now         <- Clock.now.map(_.toJava)
          commentData  = Comment.Data(article.id, CommentBody("A test comment"), userId, CreatedAt(now), UpdatedAt(now))
          _           <- database.transaction(persistence.comments.save(commentData))
          request      = ListCommentsRequest(
                           requester = User.Anonymous,
                           slug = article.slug,
                         )
          response    <- ListCommentsUseCase(database, persistence).apply(request)
        yield assert(
          response.comments.size == 1 &&
          response.comments.head.body == "A test comment",
          "Expected comments list to contain the created comment",
        )
      }

      "fail with ArticleNotFound when listing comments on a non-existent article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          request      = ListCommentsRequest(
                           requester = User.Anonymous,
                           slug = "nonexistent-article",
                         )

          result <- Abort.run(ListCommentsUseCase(database, persistence).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains("Article with slug nonexistent-article not found"),
          "Expected error message to indicate article not found",
        )
      }
    }
}
