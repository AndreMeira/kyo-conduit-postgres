package conduit.infrastructure.postgres

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import PostgresTestSupport.withDatabase
import conduit.infrastructure.TestFixtures
import kyo.*

object PostgresTagRepositorySpec extends KyoTestSuite:

  private val persistence = PostgresTestSupport.makePersistence
  private val fixtures    = TestFixtures(persistence)

  def specSuite: SuiteResult < (Async & Scope) =
    "PostgresTagRepository" should withDatabase { database =>

      "add and find tags for an article" in
        database.withMigration:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              tags     = List("scala", "kyo", "postgres")
              _       <- persistence.tags.add(article.id, tags)
              found   <- persistence.tags.find(article.id)
            yield assert(found.toSet == tags.toSet, s"Expected $tags, got $found")

      "find returns empty list for an article with no tags" in
        database.withMigration:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              found   <- persistence.tags.find(article.id)
            yield assert(found.isEmpty, s"Expected empty, got $found")

      "add is idempotent on duplicate tags" in
        database.withMigration:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              _       <- persistence.tags.add(article.id, List("a", "b"))
              _       <- persistence.tags.add(article.id, List("a", "b", "c"))
              found   <- persistence.tags.find(article.id)
            yield assert(found.toSet == Set("a", "b", "c"), s"Expected {a,b,c}, got $found")

      "add with an empty list is a no-op" in
        database.withMigration:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              _       <- persistence.tags.add(article.id, List.empty)
              found   <- persistence.tags.find(article.id)
            yield assert(found.isEmpty, s"Expected empty, got $found")

      "delete removes only the specified tags" in
        database.withMigration:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              _       <- persistence.tags.add(article.id, List("a", "b", "c"))
              _       <- persistence.tags.delete(article.id, List("a", "c"))
              found   <- persistence.tags.find(article.id)
            yield assert(found == List("b"), s"Expected [b], got $found")

      "delete with an empty list is a no-op" in
        database.withMigration:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              _       <- persistence.tags.add(article.id, List("keep"))
              _       <- persistence.tags.delete(article.id, List.empty)
              found   <- persistence.tags.find(article.id)
            yield assert(found == List("keep"), s"Expected [keep], got $found")

      "findAll includes tags added across articles" in
        database.withMigration:
          database.transaction:
            for
              userId  <- fixtures.makeUser
              _       <- fixtures.makeProfile(userId)
              article <- fixtures.makeArticle(userId)
              unique   = s"unique-${java.util.UUID.randomUUID()}"
              _       <- persistence.tags.add(article.id, List(unique))
              all     <- persistence.tags.findAll
            yield assert(all.contains(unique), s"Expected findAll to contain $unique")
    }
