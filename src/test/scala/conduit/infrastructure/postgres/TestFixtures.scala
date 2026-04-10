package conduit.infrastructure.postgres

import conduit.domain.error.ApplicationError
import conduit.domain.model.*
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import kyo.*

import java.time.temporal.ChronoUnit

/**
 * Shared helpers to build test data inside Postgres repository integration specs.
 *
 * All helpers run within an active transaction (they rely on
 * `Env[PostgresTransaction]`), so they must be called inside
 * `database.transaction { ... }` in a test body.
 */
class TestFixtures(persistence: Persistence[PostgresTransaction]):

  type Effect = Async & Abort[ApplicationError] & Env[PostgresTransaction]

  /**
   * Current time truncated to microseconds — Postgres TIMESTAMPTZ only stores
   * microsecond precision, so we truncate up front to keep round-tripped
   * timestamps equal to the originals.
   */
  def now: java.time.Instant < Sync =
    Clock.now.map(_.toJava.truncatedTo(ChronoUnit.MICROS))

  /**
   * Creates a new user row (via the credentials repository) with a unique
   * email and returns the generated userId.
   */
  def makeUser: User.Id < Effect =
    for
      userId <- IdGeneratorService.uuid
      _      <- persistence
                  .credentials
                  .save(
                    userId,
                    Credentials.Hashed(s"$userId@test.com", "hashed_password"),
                  )
    yield userId

  /**
   * Creates a new profile for an existing user and returns it. The profile
   * name defaults to a UUID-based unique value so multiple profiles in a
   * single test do not clash.
   */
  def makeProfile(userId: User.Id, name: Option[String] = None): UserProfile < Effect =
    for
      profileId <- IdGeneratorService.uuid
      ts        <- now
      profile    = UserProfile(
                     id = profileId,
                     userId = userId,
                     name = name.getOrElse(s"user-$profileId"),
                     biography = Maybe.Present("a bio"),
                     image = Maybe.Absent,
                     createdAt = ts,
                     updatedAt = ts,
                   )
      _         <- persistence.users.save(profile)
    yield profile

  /**
   * Creates an article authored by the given user and returns it. Uses
   * IdGeneratorService.slug to guarantee a unique slug per invocation.
   */
  def makeArticle(authorId: User.Id, title: String = "Hello World"): Article < Effect =
    for
      articleId <- IdGeneratorService.uuid
      slug      <- IdGeneratorService.slug(title)
      ts        <- now
      article    = Article(
                     id = articleId,
                     slug = slug,
                     title = title,
                     description = "A short description",
                     body = "The body of the article",
                     authorId = authorId,
                     favoriteCount = 0,
                     tags = List.empty,
                     createdAt = ts,
                     updatedAt = ts,
                   )
      _         <- persistence.articles.save(article)
    yield article
