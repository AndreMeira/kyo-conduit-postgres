package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.domain.model.{ User, UserProfile }
import conduit.domain.request.article.ArticleFeedRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

import java.util.UUID

object ArticleFeedUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ArticleFeedUseCase" should {
      "return articles from followed authors" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          readerId    <- database.transaction(fixtures.makeUser)
          _           <- database.transaction(fixtures.makeProfile(readerId))
          authorId    <- database.transaction(fixtures.makeUser)
          authorProf  <- database.transaction(fixtures.makeProfile(authorId))
          _           <- database.transaction(persistence.followers.add(UserProfile.Follower(readerId, authorProf.id)))
          article     <- database.transaction(fixtures.makeArticle(authorId))
          request      = ArticleFeedRequest(
                           requester = User.Authenticated(readerId),
                           offset = 0,
                           limit = 20,
                         )
          response    <- ArticleFeedUseCase(database, persistence).apply(request)
        yield
          assert(response.articlesCount == 1, "Expected articlesCount to be 1") &
          assert(response.articles.head.slug == article.slug, "Expected feed to contain the followed author's article")
      }

      "return empty list when user follows nobody" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- IdGeneratorService.uuid
          request      = ArticleFeedRequest(
                           requester = User.Authenticated(userId),
                           offset = 0,
                           limit = 20,
                         )
          response    <- ArticleFeedUseCase(database, persistence).apply(request)
        yield
          assert(response.articlesCount == 0, "Expected articlesCount to be 0") &
          assert(response.articles.isEmpty, "Expected articles list to be empty")
      }
    }
}
