package conduit.infrastructure

import conduit.domain.error.ApplicationError
import conduit.domain.model.*
import conduit.domain.types.*
import conduit.domain.service.persistence.{ Database, IdGeneratorService, Persistence }
import kyo.*

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Shared helpers to build test data inside repository integration specs.
 *
 * Parameterised on the transaction type so the same fixtures work for both
 * Postgres and in-memory repositories. All helpers rely on `Env[Tx]`, so
 * they must be called inside `database.transaction { ... }` in a test body.
 *
 * Subclasses may override [[now]] to adjust timestamp precision (e.g.
 * Postgres truncates to microseconds).
 */
class TestFixtures[Tx <: Database.Transaction](persistence: Persistence[Tx]):

  type Effect = Async & Abort[ApplicationError] & Env[Tx]

  /**
   * Current time. Override in subclasses when the storage backend has
   * limited precision (e.g. Postgres TIMESTAMPTZ → microseconds).
   */
  def now: Instant < Sync =
    Clock.now.map(_.toJava.truncatedTo(ChronoUnit.MICROS))

  /**
   * Creates a new user row (via the credentials repository) with a unique
   * email and returns the generated userId.
   */
  def makeUser: User.Id < Effect =
    for
      userId <- IdGeneratorService.uuid.map(UserId(_))
      _      <- persistence.credentials.save(
                  userId,
                  Credentials.Hashed(Email(s"$userId@test.com"), Password("hashed_password")),
                )
    yield userId

  /**
   * Creates a new profile for an existing user and returns it. The profile
   * name defaults to a UUID-based unique value so multiple profiles in a
   * single test do not clash.
   */
  def makeProfile(userId: User.Id, name: Option[String] = None): UserProfile < Effect =
    for
      profileId <- IdGeneratorService.uuid.map(UserProfileId(_))
      ts        <- now
      profile    = UserProfile(
                     id = profileId,
                     userId = userId,
                     name = ProfileName(name.getOrElse(s"user-$profileId")),
                     biography = Maybe.Present(ProfileBiography("a bio")),
                     image = Maybe.Absent,
                     createdAt = CreatedAt(ts),
                     updatedAt = UpdatedAt(ts),
                   )
      _         <- persistence.users.save(profile)
    yield profile

  /**
   * Creates an article authored by the given user and returns it. Uses
   * IdGeneratorService.slug to guarantee a unique slug per invocation.
   */
  def makeArticle(authorId: User.Id, title: String = "Hello World"): Article < Effect =
    for
      articleId <- IdGeneratorService.uuid.map(ArticleId(_))
      slug      <- IdGeneratorService.slug(title).map(ArticleSlug(_))
      ts        <- now
      article    = Article(
                     id = articleId,
                     slug = slug,
                     title = ArticleTitle(title),
                     description = ArticleDescription("A short description"),
                     body = ArticleBody("The body of the article"),
                     authorId = authorId,
                     favoriteCount = FavoriteCount(0),
                     tags = List.empty,
                     createdAt = CreatedAt(ts),
                     updatedAt = UpdatedAt(ts),
                   )
      _         <- persistence.articles.save(article.data)
    yield article
