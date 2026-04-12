package conduit.infrastructure.inmemory

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.Article
import conduit.domain.service.persistence.Persistence
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.TestFixtures
import kyo.*

object InMemoryFavoriteRepositorySpec extends KyoTestSuite:

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  def specSuite: SuiteResult < (Async & Scope) =

    "InMemoryFavoriteRepository" should {
      "add and detect a favorite" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            authorId    <- fixtures.makeUser
            _           <- fixtures.makeProfile(authorId)
            userId      <- fixtures.makeUser
            article     <- fixtures.makeArticle(authorId)
            fav          = Article.FavoriteBy(userId, article.id)
            before      <- persistence.favorites.exists(fav)
            _           <- persistence.favorites.add(fav)
            after       <- persistence.favorites.exists(fav)
          yield assert(!before, "should not exist before add") &
            assert(after, "should exist after add")
      }

      "add is idempotent" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            authorId    <- fixtures.makeUser
            _           <- fixtures.makeProfile(authorId)
            userId      <- fixtures.makeUser
            article     <- fixtures.makeArticle(authorId)
            fav          = Article.FavoriteBy(userId, article.id)
            _           <- persistence.favorites.add(fav)
            _           <- persistence.favorites.add(fav) // must not fail
            exists      <- persistence.favorites.exists(fav)
          yield assert(exists, "should still exist after double-add")
      }

      "delete a favorite" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            authorId    <- fixtures.makeUser
            _           <- fixtures.makeProfile(authorId)
            userId      <- fixtures.makeUser
            article     <- fixtures.makeArticle(authorId)
            fav          = Article.FavoriteBy(userId, article.id)
            _           <- persistence.favorites.add(fav)
            _           <- persistence.favorites.delete(fav)
            exists      <- persistence.favorites.exists(fav)
          yield assert(!exists, "should not exist after delete")
      }

      "favoriteOf returns only favorited article ids" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            authorId    <- fixtures.makeUser
            _           <- fixtures.makeProfile(authorId)
            userId      <- fixtures.makeUser
            a1          <- fixtures.makeArticle(authorId, "A1")
            a2          <- fixtures.makeArticle(authorId, "A2")
            a3          <- fixtures.makeArticle(authorId, "A3")
            _           <- persistence.favorites.add(Article.FavoriteBy(userId, a1.id))
            _           <- persistence.favorites.add(Article.FavoriteBy(userId, a3.id))
            result      <- persistence.favorites.favoriteOf(userId, List(a1.id, a2.id, a3.id))
          yield assert(result.toSet == Set(a1.id, a3.id), s"Expected {a1,a3}, got $result")
      }

      "favoriteOf returns empty for empty input" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            result      <- persistence.favorites.favoriteOf(userId, List.empty)
          yield assert(result.isEmpty, s"Expected empty, got $result")
      }

      "saved favorite increments the article's favoriteCount on read" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            authorId    <- fixtures.makeUser
            _           <- fixtures.makeProfile(authorId)
            userId      <- fixtures.makeUser
            article     <- fixtures.makeArticle(authorId)
            _           <- persistence.favorites.add(Article.FavoriteBy(userId, article.id))
            found       <- persistence.articles.find(article.id)
          yield assert(found.exists(_.favoriteCount == 1), s"Expected favoriteCount=1, got $found")
      }
    }
