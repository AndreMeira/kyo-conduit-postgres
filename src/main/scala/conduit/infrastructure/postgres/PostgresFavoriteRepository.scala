package conduit.infrastructure.postgres

import com.augustnagro.magnum.*
import conduit.domain.model.{ Article, User }
import conduit.domain.service.persistence.FavoriteRepository
import conduit.infrastructure.codecs.database.DatabaseCodecs.given
import conduit.infrastructure.postgres.PostgresTransaction.Transactional
import kyo.*

import java.util.UUID

class PostgresFavoriteRepository extends FavoriteRepository[PostgresTransaction]:

  /**
   * Checks if a favorite entry for an article by a user exists.
   *
   * @param favorite the favorite entry to check
   * @return true if the favorite entry exists, false otherwise
   */
  override def exists(favorite: Article.Favorite): Boolean < Effect =
    Transactional:
      sql"""SELECT EXISTS(
              SELECT 1 FROM favorites
              WHERE user_id = ${favorite.userId}
                AND article_id = ${favorite.articleId}
            )"""
        .query[Boolean]
        .run()
        .headOption
        .contains(true)

  /**
   * Finds which of the given article IDs have been favorited by the specified user.
   *
   * @param userId the ID of the user
   * @param articleIds the list of article IDs to check
   * @return the subset of articleIds that the user has favorited
   */
  override def favoriteOf(userId: User.Id, articleIds: List[Article.Id]): List[Article.Id] < Effect =
    if articleIds.isEmpty then List.empty
    else
      Transactional:
        val ids: List[UUID] = articleIds
        sql"""SELECT article_id FROM favorites
              WHERE user_id = $userId
                AND article_id = ANY($ids)"""
          .query[Article.Id]
          .run()
          .toList

  /**
   * Adds a favorite entry for an article by a user.
   *
   * Uses ON CONFLICT DO NOTHING to make the operation idempotent.
   *
   * @param favorite the favorite entry to add
   * @return Unit on successful addition
   */
  override def add(favorite: Article.Favorite): Unit < Effect =
    Transactional {
      sql"""INSERT INTO favorites (user_id, article_id)
            VALUES (${favorite.userId}, ${favorite.articleId})
            ON CONFLICT (user_id, article_id) DO NOTHING"""
        .update
        .run()
    }.unit

  /**
   * Deletes a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to delete
   * @return Unit on successful deletion
   */
  override def delete(favorite: Article.Favorite): Unit < Effect =
    Transactional {
      sql"""DELETE FROM favorites
            WHERE user_id = ${favorite.userId}
              AND article_id = ${favorite.articleId}"""
        .update
        .run()
    }.unit
