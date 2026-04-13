package conduit.infrastructure.inmemory

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.{ Article, UserProfile }
import conduit.domain.service.persistence.ArticleRepository.SearchParam
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import conduit.infrastructure.TestFixtures
import kyo.*

/**
 * In-memory counterpart of
 * [[conduit.infrastructure.postgres.PostgresArticleRepositorySpec]] covering
 * the search and searchCount cases that were added there. Each `in` block
 * receives a fresh database and a fresh persistence layer so state is
 * guaranteed to be isolated.
 */
object InMemoryArticleRepositorySpec extends KyoTestSuite:

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def makeFixtures: TestFixtures[InMemoryTransaction] < (Sync & Scope) =
    makePersistence.map(TestFixtures(_))

  def specSuite: SuiteResult < (Async & Scope) =

    "InMemoryArticleRepository" should {
      "allow sequential transaction" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          _           <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          found       <- database.transaction(persistence.articles.find(article.id))
        yield assert(found == Maybe.Present(article), s"Expected $article but got $found")
      }

      "save and find by id" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            article     <- fixtures.makeArticle(userId)
            found       <- persistence.articles.find(article.id)
          yield assert(found == Maybe.Present(article), s"Expected $article but got $found")
      }

      "save and find by slug" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            article     <- fixtures.makeArticle(userId)
            found       <- persistence.articles.findBySlug(article.slug)
          yield assert(found == Maybe.Present(article), s"Expected $article but got $found")
      }

      "return Absent for unknown id" in withDatabase { database =>
        database.transaction:
          for
            persistence <- makePersistence
            unknownId   <- IdGeneratorService.uuid
            found       <- persistence.articles.find(unknownId)
          yield assert(found == Maybe.Absent, s"Expected Absent but got $found")
      }

      "report existence correctly" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            article     <- fixtures.makeArticle(userId)
            unknownId   <- IdGeneratorService.uuid
            existsYes   <- persistence.articles.exists(article.id)
            existsNo    <- persistence.articles.exists(unknownId)
          yield assert(existsYes, "expected exists(saved)=true") &
            assert(!existsNo, "expected exists(unknown)=false")
      }

      "save article data and find by id" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            articleId   <- IdGeneratorService.uuid
            slug        <- IdGeneratorService.slug("Data Save Test")
            ts          <- fixtures.now
            data         = Article.Data(articleId, slug, "Data Save Test", "desc", "body", userId, ts, ts)
            _           <- persistence.articles.save(data)
            found       <- persistence.articles.find(articleId)
          yield
            val expected = data.toArticle(favoriteCount = 0, tags = List.empty)
            assert(found == Maybe.Present(expected), s"Expected $expected but got $found")
      }

      "update article data" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            article     <- fixtures.makeArticle(userId, "Original")
            ts          <- fixtures.now
            updated      = article.data.copy(title = "Updated title", body = "New body", updatedAt = ts)
            _           <- persistence.articles.update(updated)
            found       <- persistence.articles.find(article.id)
          yield
            val expected = updated.toArticle(article.favoriteCount, article.tags)
            assert(found == Maybe.Present(expected), s"Expected $expected but got $found")
      }

      "update article data across transactions" in withDatabase { database =>
        for
          fixtures    <- makeFixtures
          persistence <- makePersistence
          userId      <- database.transaction(fixtures.makeUser)
          _           <- database.transaction(fixtures.makeProfile(userId))
          article     <- database.transaction(fixtures.makeArticle(userId))
          ts          <- Clock.now.map(_.toJava)
          updated      = article.data.copy(title = "Cross-tx Updated", body = "New body", slug = "cross-tx-updated", updatedAt = ts)
          _           <- database.transaction(persistence.articles.update(updated))
          foundById   <- database.transaction(persistence.articles.find(article.id))
          foundBySlug <- database.transaction(persistence.articles.findBySlug("cross-tx-updated"))
          oldSlug     <- database.transaction(persistence.articles.findBySlug(article.slug))
        yield
          val result = foundById.toOption.get
          assert(result.title == "Cross-tx Updated", s"Expected updated title, got ${result.title}") &
          assert(result.body == "New body", s"Expected updated body, got ${result.body}") &
          assert(result.slug == "cross-tx-updated", s"Expected updated slug, got ${result.slug}") &
          assert(foundBySlug.isDefined, s"Expected to find article by new slug") &
          assert(oldSlug.isEmpty, s"Expected old slug to no longer resolve")
      }

      "update article data preserves favorite count and tags" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            article     <- fixtures.makeArticle(userId, "With extras")
            _           <- persistence.tags.add(article.id, List("scala", "kyo"))
            _           <- persistence.favorites.add(Article.Favorite(userId, article.id))
            before      <- persistence.articles.find(article.id)
            ts          <- fixtures.now
            updated      = article.data.copy(title = "New title", updatedAt = ts)
            _           <- persistence.articles.update(updated)
            after       <- persistence.articles.find(article.id)
          yield
            val result = after.toOption.get
            assert(result.favoriteCount == 1, s"Expected favoriteCount=1 but got ${result.favoriteCount}") &
            assert(result.tags.sorted == List("kyo", "scala"), s"Expected tags preserved but got ${result.tags}") &
            assert(result.title == "New title", s"Expected updated title")
      }

      "search by author username" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            u1          <- fixtures.makeUser
            p1          <- fixtures.makeProfile(u1)
            u2          <- fixtures.makeUser
            _           <- fixtures.makeProfile(u2)
            a1          <- fixtures.makeArticle(u1, "A1")
            _           <- fixtures.makeArticle(u2, "A2")
            result      <- persistence.articles.search(List(SearchParam.Author(p1.name)), 0, 10)
          yield assert(result.map(_.id).contains(a1.id), s"expected a1 in results: $result") &
            assert(result.forall(_.authorId == u1), s"expected results only from u1: $result")
      }

      "search by tag" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            article     <- fixtures.makeArticle(userId, "Tagged")
            tagId       <- conduit.domain.service.persistence.IdGeneratorService.uuid
            _           <- persistence.tags.add(article.id, List(s"tag-$tagId"))
            result      <- persistence.articles.search(List(SearchParam.Tag(s"tag-$tagId")), 0, 10)
          yield assert(
            result.map(_.id) == List(article.id),
            s"Expected [${article.id}], got ${result.map(_.id)}",
          )
      }

      "search by favoriter username" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            authorId    <- fixtures.makeUser
            _           <- fixtures.makeProfile(authorId)
            favUserId   <- fixtures.makeUser
            favProf     <- fixtures.makeProfile(favUserId)
            article     <- fixtures.makeArticle(authorId, "Favorited")
            _           <- persistence.favorites.add(conduit.domain.model.Article.Favorite(favUserId, article.id))
            result      <- persistence.articles.search(List(SearchParam.FavoriteBy(favProf.name)), 0, 10)
          yield assert(
            result.map(_.id).contains(article.id),
            s"Expected article in favorite search: $result",
          )
      }

      "search with no filters returns all articles" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            u1          <- fixtures.makeUser
            _           <- fixtures.makeProfile(u1)
            u2          <- fixtures.makeUser
            _           <- fixtures.makeProfile(u2)
            a1          <- fixtures.makeArticle(u1, "A1")
            a2          <- fixtures.makeArticle(u1, "A2")
            a3          <- fixtures.makeArticle(u2, "A3")
            result      <- persistence.articles.search(Nil, 0, 10)
          yield assert(
            result.map(_.id).toSet == Set(a1.id, a2.id, a3.id),
            s"Expected all 3 articles, got ${result.map(_.id)}",
          )
      }

      "search returns empty when no article matches" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            _           <- fixtures.makeArticle(userId)
            result      <- persistence.articles.search(List(SearchParam.Author("ghost")), 0, 10)
          yield assert(result.isEmpty, s"Expected empty result, got $result")
      }

      "search combines filters with AND semantics" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            u1          <- fixtures.makeUser
            p1          <- fixtures.makeProfile(u1)
            u2          <- fixtures.makeUser
            _           <- fixtures.makeProfile(u2)
            a1          <- fixtures.makeArticle(u1, "Matches both")
            _           <- fixtures.makeArticle(u1, "Wrong tag")
            a3          <- fixtures.makeArticle(u2, "Wrong author")
            tagId       <- conduit.domain.service.persistence.IdGeneratorService.uuid
            tag          = s"tag-$tagId"
            _           <- persistence.tags.add(a1.id, List(tag))
            _           <- persistence.tags.add(a3.id, List(tag))
            result      <- persistence.articles.search(List(SearchParam.Author(p1.name), SearchParam.Tag(tag)), 0, 10)
          yield assert(
            result.map(_.id) == List(a1.id),
            s"Expected only [${a1.id}] matching both filters, got ${result.map(_.id)}",
          )
      }

      "search respects offset and limit" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            _           <- fixtures.makeArticle(userId, "A1")
            _           <- fixtures.makeArticle(userId, "A2")
            _           <- fixtures.makeArticle(userId, "A3")
            page1       <- persistence.articles.search(Nil, offset = 0, limit = 2)
            page2       <- persistence.articles.search(Nil, offset = 2, limit = 2)
          yield assert(page1.size == 2, s"Expected page1 size 2, got ${page1.size}") &
            assert(page2.size == 1, s"Expected page2 size 1, got ${page2.size}") &
            assert(
              (page1.map(_.id) ++ page2.map(_.id)).toSet.size == 3,
              s"Expected pages to cover 3 distinct articles, got ${page1.map(_.id)} and ${page2.map(_.id)}",
            )
      }

      "search orders results by creation date descending" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            a1          <- fixtures.makeArticle(userId, "First")
            a2          <- fixtures.makeArticle(userId, "Second")
            a3          <- fixtures.makeArticle(userId, "Third")
            result      <- persistence.articles.search(Nil, 0, 10)
          yield assert(
            result.map(_.createdAt) == result.map(_.createdAt).sorted(using Ordering[java.time.Instant].reverse),
            s"Expected results ordered by createdAt DESC, got ${result.map(a => a.title -> a.createdAt)}",
          ) & assert(
            result.map(_.id).toSet == Set(a1.id, a2.id, a3.id),
            s"Expected all 3 articles, got ${result.map(_.id)}",
          )
      }

      "searchCount with no filters counts all articles" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            u1          <- fixtures.makeUser
            _           <- fixtures.makeProfile(u1)
            u2          <- fixtures.makeUser
            _           <- fixtures.makeProfile(u2)
            _           <- fixtures.makeArticle(u1, "A1")
            _           <- fixtures.makeArticle(u1, "A2")
            _           <- fixtures.makeArticle(u2, "A3")
            count       <- persistence.articles.searchCount(Nil)
          yield assert(count == 3, s"Expected 3 articles, got $count")
      }

      "searchCount by author counts only that author's articles" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            u1          <- fixtures.makeUser
            p1          <- fixtures.makeProfile(u1)
            u2          <- fixtures.makeUser
            _           <- fixtures.makeProfile(u2)
            _           <- fixtures.makeArticle(u1, "A1")
            _           <- fixtures.makeArticle(u1, "A2")
            _           <- fixtures.makeArticle(u2, "A3")
            count       <- persistence.articles.searchCount(List(SearchParam.Author(p1.name)))
          yield assert(count == 2, s"Expected 2 articles by ${p1.name}, got $count")
      }

      "searchCount by tag counts only tagged articles" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            a1          <- fixtures.makeArticle(userId, "Tagged one")
            a2          <- fixtures.makeArticle(userId, "Tagged two")
            _           <- fixtures.makeArticle(userId, "Untagged")
            tagId       <- conduit.domain.service.persistence.IdGeneratorService.uuid
            tag          = s"tag-$tagId"
            _           <- persistence.tags.add(a1.id, List(tag))
            _           <- persistence.tags.add(a2.id, List(tag))
            count       <- persistence.articles.searchCount(List(SearchParam.Tag(tag)))
          yield assert(count == 2, s"Expected 2 articles tagged '$tag', got $count")
      }

      "searchCount by favoriter counts only favorited articles" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            authorId    <- fixtures.makeUser
            _           <- fixtures.makeProfile(authorId)
            favUserId   <- fixtures.makeUser
            favProf     <- fixtures.makeProfile(favUserId)
            a1          <- fixtures.makeArticle(authorId, "Liked one")
            a2          <- fixtures.makeArticle(authorId, "Liked two")
            _           <- fixtures.makeArticle(authorId, "Unliked")
            _           <- persistence.favorites.add(conduit.domain.model.Article.Favorite(favUserId, a1.id))
            _           <- persistence.favorites.add(conduit.domain.model.Article.Favorite(favUserId, a2.id))
            count       <- persistence.articles.searchCount(List(SearchParam.FavoriteBy(favProf.name)))
          yield assert(count == 2, s"Expected 2 articles favorited by ${favProf.name}, got $count")
      }

      "searchCount returns 0 when no article matches" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            _           <- fixtures.makeArticle(userId)
            count       <- persistence.articles.searchCount(List(SearchParam.Author("ghost")))
          yield assert(count == 0, s"Expected 0 articles, got $count")
      }

      "searchCount combines filters with AND semantics" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            u1          <- fixtures.makeUser
            p1          <- fixtures.makeProfile(u1)
            u2          <- fixtures.makeUser
            _           <- fixtures.makeProfile(u2)
            a1          <- fixtures.makeArticle(u1, "Matches both")
            _           <- fixtures.makeArticle(u1, "Wrong tag")
            a3          <- fixtures.makeArticle(u2, "Wrong author")
            tagId       <- conduit.domain.service.persistence.IdGeneratorService.uuid
            tag          = s"tag-$tagId"
            _           <- persistence.tags.add(a1.id, List(tag))
            _           <- persistence.tags.add(a3.id, List(tag))
            count       <- persistence.articles.searchCount:
                             List(SearchParam.Author(p1.name), SearchParam.Tag(tag))
          yield assert(count == 1, s"Expected 1 article matching both filters, got $count")
      }

      "searchCount is independent of pagination" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            _           <- fixtures.makeArticle(userId, "A1")
            _           <- fixtures.makeArticle(userId, "A2")
            _           <- fixtures.makeArticle(userId, "A3")
            page        <- persistence.articles.search(Nil, offset = 0, limit = 1)
            count       <- persistence.articles.searchCount(Nil)
          yield assert(page.size == 1, s"Expected page size 1, got ${page.size}") &
            assert(count == 3, s"Expected total count 3 independent of page size, got $count")
      }

      "feedOf returns articles from followed authors only" in withDatabase { database =>
        database.transaction:
          for
            fixtures      <- makeFixtures
            persistence   <- makePersistence
            followerId    <- fixtures.makeUser
            _             <- fixtures.makeProfile(followerId)
            authorId      <- fixtures.makeUser
            authorProfile <- fixtures.makeProfile(authorId)
            otherId       <- fixtures.makeUser
            _             <- fixtures.makeProfile(otherId)
            _             <- persistence.followers.add(UserProfile.Follower(followerId, authorProfile.id))
            a1            <- fixtures.makeArticle(authorId, "From followee")
            _             <- fixtures.makeArticle(otherId, "From other")
            feed          <- persistence.articles.feedOf(followerId, 0, 10)
            count         <- persistence.articles.countFeedOf(followerId)
          yield assert(feed.map(_.id) == List(a1.id), s"Expected [a1], got ${feed.map(_.id)}") &
            assert(count == 1, s"Expected count=1, got $count")
      }

      "feedOf returns empty for user who follows no one" in withDatabase { database =>
        database.transaction:
          for
            fixtures    <- makeFixtures
            persistence <- makePersistence
            userId      <- fixtures.makeUser
            _           <- fixtures.makeProfile(userId)
            feed        <- persistence.articles.feedOf(userId, 0, 10)
            count       <- persistence.articles.countFeedOf(userId)
          yield assert(feed.isEmpty, s"Expected empty feed, got $feed") &
            assert(count == 0, s"Expected count=0, got $count")
      }
    }
