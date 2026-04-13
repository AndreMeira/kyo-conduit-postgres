package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Comment, User }
import conduit.domain.request.comment.DeleteCommentRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object CommentDeletionUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "CommentDeletionUseCase" should {
      "succeed in deleting an existing comment" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          now         <- Clock.now.map(_.toJava)
          userId      <- database.transaction(fixtures.makeUser)
          _           <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          commentData  = Comment.Data(article.id, "A comment", userId, now, now)
          comment     <- database.transaction(persistence.comments.save(commentData))
          request      = DeleteCommentRequest(
                           requester = User.Authenticated(userId),
                           slug = article.slug,
                           commentId = comment.id,
                         )
          response    <- CommentDeletionUseCase(database, persistence).apply(request)
          found       <- database.transaction(persistence.comments.find(comment.id))
        yield ok &
          assert(response.comment == comment.id, "Expected deleted comment id to match") &
          assert(found.isEmpty, "Expected comment to be deleted and not found in database")
      }

      "fail with ArticleNotFound when deleting a comment on a non-existent article" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid
          request      = DeleteCommentRequest(
                           requester = User.Authenticated(userId),
                           slug = "nonexistent-article",
                           commentId = 1L,
                         )

          result <- Abort.run(CommentDeletionUseCase(database, persistence).apply(request))
          error   = result.toEither.swap.toOption.collect { case e: ApplicationError => e }.get
        yield assert(
          error.message.contains("Article with slug nonexistent-article not found"),
          "Expected error message to indicate article not found",
        )
      }
    }
}
