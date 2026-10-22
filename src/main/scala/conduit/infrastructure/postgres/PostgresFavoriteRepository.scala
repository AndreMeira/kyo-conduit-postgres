package conduit.infrastructure.postgres

import conduit.domain.model.{ Article, User }
import conduit.domain.service.persistence.FavoriteRepository
import conduit.infrastructure.codecs.database.SqlSchemaCodecs.given
import PostgresTransaction.Transactional
import kyo.*

/**
 * Postgres implementation of [[FavoriteRepository]], over the `favorites` table.
 */
class PostgresFavoriteRepository extends FavoriteRepository[PostgresTransaction]:

  /**
   * Whether the user has favorited the article.
   *
   * @param favorite the user and the article
   * @return true when the favorite is recorded; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def exists(favorite: Article.Favorite): Boolean < Effect =
    Transactional:
      Sql.from[Article.Favorite]("f", "favorites")
        .where(row => row.f.userId == favorite.userId && row.f.articleId == favorite.articleId)
        .limit(1)
        .run
        .map(_.nonEmpty)

  /**
   * The articles, among those given, that the user has favorited.
   *
   * @param userId the user
   * @param articleIds the articles to check
   * @return the favorited ones; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def favoriteOf(userId: User.Id, articleIds: List[Article.Id]): List[Article.Id] < Effect =
    Transactional:
      Sql.from[Article.Favorite]("f", "favorites")
        .where(row => row.f.userId == userId && row.f.articleId.in(articleIds*))
        .select(_.f.articleId)
        .run
        .map(_.toList)

  /**
   * Records a favorite; one already recorded is left as it is.
   *
   * @param favorite the user and the article
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the insert fails
   */
  override def add(favorite: Article.Favorite): Unit < Effect =
    Transactional:
      Sql.insert[Article.Favorite]("favorites")
        .values(favorite)
        .onConflictDoNothing(_.userId, _.articleId)
        .run
        .unit

  /**
   * Removes a favorite; one not recorded is ignored.
   *
   * @param favorite the user and the article
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the delete fails
   */
  override def delete(favorite: Article.Favorite): Unit < Effect =
    Transactional:
      Sql.delete[Article.Favorite]("favorites")
        .where(row => row.userId == favorite.userId && row.articleId == favorite.articleId)
        .run
        .unit
