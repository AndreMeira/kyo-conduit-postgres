package conduit.infrastructure.postgres

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.{ Article, UserProfile }
import conduit.domain.service.persistence.ArticleRepository.SearchParam
import conduit.domain.service.persistence.IdGeneratorService
import PostgresTestSupport.withDatabase
import kyo.*

object PostgresArticleRepositorySpec extends KyoTestSuite:

  private val persistence = PostgresTestSupport.makePersistence
  private val fixtures    = TestFixtures(persistence)

  def specSuite: SuiteResult < (Async & Scope) =
    "PostgresArticleRepository" should withDatabase { database =>

      "save and find by id" in
        database.withCleanDatabase:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              found   <- persistence.articles.find(article.id)
            yield assert(found == Maybe.Present(article), s"Expected $article but got $found")

      "save and find by slug" in
        database.withCleanDatabase:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              found   <- persistence.articles.findBySlug(article.slug)
            yield assert(found == Maybe.Present(article), s"Expected $article but got $found")

      "return Absent for unknown id" in
        database.withCleanDatabase:
          database.transaction:
            for
              unknownId <- IdGeneratorService.uuid
              found     <- persistence.articles.find(unknownId)
            yield assert(found == Maybe.Absent, s"Expected Absent but got $found")

      "report existence correctly" in
        database.withCleanDatabase:
          database.transaction:
            for
              userId    <- fixtures.makeUser
              _         <- fixtures.makeProfile(userId)
              article   <- fixtures.makeArticle(userId)
              unknownId <- IdGeneratorService.uuid
              existsYes <- persistence.articles.exists(article.id)
              existsNo  <- persistence.articles.exists(unknownId)
            yield assert(existsYes, "expected exists(saved)=true") &
              assert(!existsNo, "expected exists(unknown)=false")

      "update article" in
        database.withCleanDatabase:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId, "Original")
              ts      <- fixtures.now
              updated  = article.copy(
                           title = "Updated title",
                           body = "New body",
                           updatedAt = ts,
                         )
              _       <- persistence.articles.update(updated)
              found   <- persistence.articles.find(article.id)
            yield assert(found == Maybe.Present(updated), s"Expected $updated but got $found")

      "search by author username" in
        database.withCleanDatabase:
          database.transaction:
            for
              u1     <- fixtures.makeUser
              p1     <- fixtures.makeProfile(u1)
              u2     <- fixtures.makeUser
              _      <- fixtures.makeProfile(u2)
              a1     <- fixtures.makeArticle(u1, "A1")
              _      <- fixtures.makeArticle(u2, "A2")
              result <- persistence.articles.search(List(SearchParam.Author(p1.name)))
            yield assert(result.map(_.id).contains(a1.id), s"expected a1 in results: $result") &
              assert(result.forall(_.authorId == u1), s"expected results only from u1: $result")

      "search by tag" in
        database.withCleanDatabase:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId, "Tagged")
              tagId   <- IdGeneratorService.uuid
              tagName  = s"tag-$tagId"
              _       <- persistence.tags.add(article.id, List(tagName))
              result  <- persistence.articles.search(List(SearchParam.Tag(tagName)))
            yield assert(
              result.map(_.id) == List(article.id),
              s"Expected [${article.id}], got ${result.map(_.id)}",
            )

      "search by favoriter username" in
        database.withCleanDatabase:
          database.transaction:
            for
              authorId  <- fixtures.makeUser
              _         <- fixtures.makeProfile(authorId)
              favUserId <- fixtures.makeUser
              favProf   <- fixtures.makeProfile(favUserId)
              article   <- fixtures.makeArticle(authorId, "Favorited")
              _         <- persistence.favorites.add(Article.FavoriteBy(favUserId, article.id))
              result    <- persistence.articles.search:
                             List(SearchParam.FavoriteBy(favProf.name))
            yield assert(
              result.map(_.id).contains(article.id),
              s"Expected article in favorite search: $result",
            )

      "feedOf returns articles from followed authors only" in
        database.withCleanDatabase:
          database.transaction:
            for
              followerId <- fixtures.makeUser
              _          <- fixtures.makeProfile(followerId)
              authorId   <- fixtures.makeUser
              _          <- fixtures.makeProfile(authorId)
              otherId    <- fixtures.makeUser
              _          <- fixtures.makeProfile(otherId)
              _          <- persistence.followers.add:
                              UserProfile.FollowedBy(followerId, authorId)
              a1         <- fixtures.makeArticle(authorId, "From followee")
              _          <- fixtures.makeArticle(otherId, "From other")
              feed       <- persistence.articles.feedOf(followerId, 0, 10)
              count      <- persistence.articles.countFeedOf(followerId)
            yield assert(feed.map(_.id) == List(a1.id), s"Expected [a1], got ${feed.map(_.id)}") &
              assert(count == 1, s"Expected count=1, got $count")

      "feedOf returns empty for user who follows no one" in
        database.withCleanDatabase:
          database.transaction:
            for
              userId <- fixtures.makeUser
              _      <- fixtures.makeProfile(userId)
              feed   <- persistence.articles.feedOf(userId, 0, 10)
              count  <- persistence.articles.countFeedOf(userId)
            yield assert(feed.isEmpty, s"Expected empty feed, got $feed") &
              assert(count == 0, s"Expected count=0, got $count")
    }
