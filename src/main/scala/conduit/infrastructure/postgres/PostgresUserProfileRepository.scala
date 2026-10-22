package conduit.infrastructure.postgres

import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.service.persistence.UserProfileRepository
import conduit.domain.types.ProfileName
import conduit.infrastructure.codecs.database.SqlSchemaCodecs.given
import PostgresTransaction.Transactional
import kyo.*

/**
 * Postgres implementation of [[UserProfileRepository]], over the `profiles` table.
 */
class PostgresUserProfileRepository extends UserProfileRepository[PostgresTransaction]:

  /**
   * Saves a new profile.
   *
   * @param profile the profile
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the insert fails
   */
  override def save(profile: UserProfile): Unit < Effect =
    Transactional.single("profiles"):
      Sql.insert[UserProfile]("profiles").values(profile).run.map(_.affectedRows)

  /**
   * Updates a profile's name, biography, image and update time.
   *
   * @param profile the profile, carrying its new values
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the update fails, or with
   *         `PostgresDatabase.Error.UnexpectedRowCount` when no profile has its id
   */
  override def update(profile: UserProfile): Unit < Effect =
    Transactional.single("profiles"):
      Sql.update[UserProfile]("profiles")
        .set(
          _.name      := profile.name,
          _.biography := profile.biography,
          _.image     := profile.image,
          _.updatedAt := profile.updatedAt,
        )
        .where(_.id == profile.id)
        .run

  /**
   * Whether a profile exists.
   *
   * @param id the profile
   * @return true when it exists; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def exists(id: UserProfile.Id): Boolean < Effect =
    Transactional:
      Sql.from[UserProfile]("p", "profiles").where(_.p.id == id).limit(1).run.map(_.nonEmpty)

  /**
   * Finds a profile by its id.
   *
   * @param id the profile
   * @return the profile, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def find(id: UserProfile.Id): Maybe[UserProfile] < Effect =
    Transactional:
      Sql.from[UserProfile]("p", "profiles").where(_.p.id == id).run.map(first)

  /**
   * Finds a user's profile.
   *
   * @param id the user
   * @return the profile, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def findByUser(id: User.Id): Maybe[UserProfile] < Effect =
    Transactional:
      Sql.from[UserProfile]("p", "profiles").where(_.p.userId == id).run.map(first)

  /**
   * Finds the profiles of the given users.
   *
   * @param ids the users
   * @return the profiles found; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def findByUsers(ids: List[User.Id]): List[UserProfile] < Effect =
    Transactional:
      Sql.from[UserProfile]("p", "profiles").where(_.p.userId.in(ids*)).run.map(_.toList)

  /**
   * Finds a profile by its username.
   *
   * @param username the username
   * @return the profile, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def findByUsername(username: String): Maybe[UserProfile] < Effect =
    Transactional:
      Sql.from[UserProfile]("p", "profiles").where(_.p.name == ProfileName(username)).run.map(first)

  /**
   * Whether a profile holds this username.
   *
   * @param username the username
   * @return true when it is taken; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def exists(username: String): Boolean < Effect =
    Transactional:
      Sql.from[UserProfile]("p", "profiles").where(_.p.name == ProfileName(username)).limit(1).run.map(_.nonEmpty)

  /**
   * Finds the profile of an article's author.
   *
   * @param id the article
   * @return the author's profile, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def findByArticle(id: Article.Id): Maybe[UserProfile] < Effect =
    Transactional:
      Sql.from[UserProfile]("p", "profiles")
        .where(_.p.userId.in(Sql.from[Article.Data]("a", "articles").where(_.a.id == id).select(_.a.authorId)))
        .run
        .map(first)

  /**
   * Finds the profiles of the authors of the given articles.
   *
   * @param ids the articles
   * @return each found article's author profile, by article; aborts with `PostgresDatabase.Error.QueryError`
   *         when the query fails
   */
  override def findByArticles(ids: Set[Article.Id]): Map[Article.Id, UserProfile] < Effect =
    Transactional:
      Sql.from[UserProfile]("p", "profiles")
        .innerJoin(Sql.from[Article.Data]("a", "articles")).on(row => row.a.authorId == row.p.userId)
        .where(_.a.id.in(ids.toSeq*))
        .select(row => (row.a.id, row.p.id, row.p.userId, row.p.name, row.p.biography, row.p.image, row.p.createdAt, row.p.updatedAt))
        .run
        .map(_.map((articleId, id, userId, name, biography, image, createdAt, updatedAt) =>
          articleId -> UserProfile(id, userId, name, biography, image, createdAt, updatedAt)).toMap)

  /**
   * The profile in the first row, if any.
   *
   * @param rows the profiles a query returned
   * @return the first one, or `Absent`
   */
  private def first(rows: Chunk[UserProfile]): Maybe[UserProfile] =
    Maybe.fromOption(rows.headOption)
