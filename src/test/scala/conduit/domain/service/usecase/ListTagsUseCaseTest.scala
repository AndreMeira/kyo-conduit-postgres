package conduit.domain.service.usecase

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.User
import conduit.domain.request.article.ListTagsRequest
import conduit.domain.service.persistence.Persistence
import conduit.infrastructure.TestFixtures
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.inmemory.{ InMemoryTestSupport, InMemoryTransaction }
import kyo.*

object ListTagsUseCaseTest extends KyoTestSuite {

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  override def specSuite: SuiteResult < (Async & Scope) =
    "ListTagsUseCase" should {
      "return tags when they exist" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          _           <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          _           <- database.transaction(persistence.tags.add(article.id, List("scala", "kyo")))
          request      = ListTagsRequest(requester = User.Anonymous)
          response    <- ListTagsUseCase(database, persistence).apply(request)
        yield assert(
          response.tags.toSet == Set("scala", "kyo"),
          "Expected tags list to contain the added tags",
        )
      }

      "return empty list when no tags exist" in withDatabase { database =>
        for
          persistence <- makePersistence
          request      = ListTagsRequest(requester = User.Anonymous)
          response    <- ListTagsUseCase(database, persistence).apply(request)
        yield assert(response.tags.isEmpty, "Expected tags list to be empty")
      }
    }
}
