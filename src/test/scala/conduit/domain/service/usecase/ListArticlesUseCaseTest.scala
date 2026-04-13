package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.User
import conduit.domain.request.article.ListArticlesRequest
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

object ListArticlesUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ListArticlesUseCase" should {
      "return articles when they exist" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          profile     <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          request      = ListArticlesRequest(
                           requester = User.Anonymous,
                           offset = 0,
                           limit = 10,
                           filters = Nil,
                         )
          response    <- ListArticlesUseCase(database, persistence).apply(request)
        yield
          assert(response.articlesCount == 1, "Expected articlesCount to be 1") &
          assert(response.articles.head.slug == article.slug, "Expected articles list to contain created article")
      }

      "return empty response when no articles exist" in withDatabase { database =>
        for
          persistence <- makePersistence
          request      = ListArticlesRequest(
                           requester = User.Anonymous,
                           offset = 0,
                           limit = 10,
                           filters = Nil,
                         )
          response    <- ListArticlesUseCase(database, persistence).apply(request)
        yield
          assert(response.articlesCount == 0, "Expected articlesCount to be 0") &
          assert(response.articles.isEmpty, "Expected articles list to be empty")
      }
    }
}
