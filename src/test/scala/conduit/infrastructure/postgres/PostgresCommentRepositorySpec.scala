package conduit.infrastructure.postgres

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.Comment
import conduit.domain.types.*
import conduit.infrastructure.postgres.PostgresTestSupport.withDatabase
import conduit.infrastructure.TestFixtures
import kyo.*

object PostgresCommentRepositorySpec extends KyoTestSuite:

  private val persistence = PostgresTestSupport.makePersistence
  private val fixtures    = TestFixtures(persistence)

  def specSuite: SuiteResult < (Async & Scope) =
    "PostgresCommentRepository" should withDatabase { database =>

      "save and find a comment" in
        database.withMigration:
          database.transaction:
            for
              authorId <- fixtures.makeUser
              _        <- fixtures.makeProfile(authorId)
              article  <- fixtures.makeArticle(authorId)
              ts       <- fixtures.now
              data      = Comment.Data(
                            articleId = article.id,
                            body = CommentBody("Nice article!"),
                            authorId = authorId,
                            createdAt = CreatedAt(ts),
                            updatedAt = UpdatedAt(ts),
                          )
              saved    <- persistence.comments.save(data)
              found    <- persistence.comments.find(saved.id)
            yield assert(found == Maybe.Present(saved), s"Expected $saved but got $found") &
              assert(saved.body == "Nice article!", s"Expected body round-trip, got ${saved.body}")

      "save assigns distinct DB-generated ids" in
        database.withMigration:
          database.transaction:
            for
              authorId <- fixtures.makeUser
              _        <- fixtures.makeProfile(authorId)
              article  <- fixtures.makeArticle(authorId)
              ts       <- fixtures.now
              data      = Comment.Data(article.id, CommentBody("hi"), authorId, CreatedAt(ts), UpdatedAt(ts))
              c1       <- persistence.comments.save(data)
              c2       <- persistence.comments.save(data)
            yield assert(c1.id != c2.id, s"Expected distinct ids but got ${c1.id} and ${c2.id}")

      "exists returns true for a saved comment and false otherwise" in
        database.withMigration:
          database.transaction:
            for
              authorId  <- fixtures.makeUser
              _         <- fixtures.makeProfile(authorId)
              article   <- fixtures.makeArticle(authorId)
              ts        <- fixtures.now
              saved     <- persistence.comments.save(
                             Comment.Data(article.id, CommentBody("x"), authorId, CreatedAt(ts), UpdatedAt(ts))
                           )
              existsYes <- persistence.comments.exists(saved.id)
              existsNo  <- persistence.comments.exists(CommentId(Long.MaxValue))
            yield assert(existsYes, "expected exists(saved)=true") &
              assert(!existsNo, "expected exists(unknown)=false")

      "findByArticleId returns comments in creation order" in
        database.withMigration:
          database.transaction:
            for
              authorId <- fixtures.makeUser
              _        <- fixtures.makeProfile(authorId)
              article  <- fixtures.makeArticle(authorId)
              ts       <- fixtures.now
              c1       <- persistence.comments.save(
                            Comment.Data(article.id, CommentBody("one"), authorId, CreatedAt(ts), UpdatedAt(ts))
                          )
              c2       <- persistence.comments.save(
                            Comment.Data(
                              article.id,
                              CommentBody("two"),
                              authorId,
                              CreatedAt(ts.plusMillis(1)),
                              UpdatedAt(ts.plusMillis(1)),
                            )
                          )
              c3       <- persistence.comments.save(
                            Comment.Data(
                              article.id,
                              CommentBody("three"),
                              authorId,
                              CreatedAt(ts.plusMillis(2)),
                              UpdatedAt(ts.plusMillis(2)),
                            )
                          )
              all      <- persistence.comments.findByArticleId(article.id)
            yield assert(
              all.map(_.id) == List(c1.id, c2.id, c3.id),
              s"Expected [c1,c2,c3], got ${all.map(_.id)}",
            )

      "findByArticleId returns empty for an article without comments" in
        database.withMigration:
          database.transaction:
            for
              authorId <- fixtures.makeUser
              _        <- fixtures.makeProfile(authorId)
              article  <- fixtures.makeArticle(authorId)
              all      <- persistence.comments.findByArticleId(article.id)
            yield assert(all.isEmpty, s"Expected empty, got $all")

      "update changes the comment body" in
        database.withMigration:
          database.transaction:
            for
              authorId <- fixtures.makeUser
              _        <- fixtures.makeProfile(authorId)
              article  <- fixtures.makeArticle(authorId)
              ts       <- fixtures.now
              saved    <- persistence.comments.save(
                            Comment.Data(article.id, CommentBody("old"), authorId, CreatedAt(ts), UpdatedAt(ts))
                          )
              updated   = saved.copy(body = CommentBody("new"), updatedAt = UpdatedAt(ts.plusSeconds(1)))
              _        <- persistence.comments.update(updated)
              found    <- persistence.comments.find(saved.id)
            yield assert(found == Maybe.Present(updated), s"Expected $updated but got $found")

      "delete removes a comment" in
        database.withMigration:
          database.transaction:
            for
              authorId <- fixtures.makeUser
              _        <- fixtures.makeProfile(authorId)
              article  <- fixtures.makeArticle(authorId)
              ts       <- fixtures.now
              saved    <- persistence.comments.save(
                            Comment.Data(article.id, CommentBody("bye"), authorId, CreatedAt(ts), UpdatedAt(ts))
                          )
              _        <- persistence.comments.delete(saved.id)
              found    <- persistence.comments.find(saved.id)
            yield assert(found == Maybe.Absent, s"Expected Emtpy, got $found")
    }
