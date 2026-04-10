package conduit.infrastructure.postgres

import com.augustnagro.magnum.*
import conduit.domain.model.Article
import conduit.domain.service.persistence.TagRepository
import conduit.infrastructure.codecs.database.DatabaseCodecs.given
import conduit.infrastructure.postgres.PostgresTransaction.Transactional
import kyo.*

class PostgresTagRepository extends TagRepository[PostgresTransaction]:

  /**
   * Retrieves all distinct tags in the system, ordered alphabetically.
   *
   * @return a list of all unique tag strings
   */
  override def findAll: List[String] < Effect =
    Transactional:
      sql"""SELECT DISTINCT name FROM tags ORDER BY name"""
        .query[String]
        .run()
        .toList

  /**
   * Associates one or more tags with an article.
   *
   * Uses unnest to batch-insert all tags in a single statement.
   * ON CONFLICT DO NOTHING makes the operation idempotent.
   *
   * @param articleId the ID of the article to add tags to
   * @param tags the list of tag strings to associate with the article
   * @return Unit on successful addition
   */
  override def add(articleId: Article.Id, tags: List[String]): Unit < Effect =
    if tags.isEmpty then ()
    else
      Transactional {
        sql"""INSERT INTO tags (name, article_id)
              SELECT unnest($tags), $articleId
              ON CONFLICT (name, article_id) DO NOTHING"""
          .update
          .run()
      }.unit

  /**
   * Retrieves all tags associated with a specific article.
   *
   * @param articleId the ID of the article to retrieve tags for
   * @return a list of tag strings associated with the specified article
   */
  override def find(articleId: Article.Id): List[String] < Effect =
    Transactional:
      sql"""SELECT name FROM tags WHERE article_id = $articleId"""
        .query[String]
        .run()
        .toList

  /**
   * Removes specified tags from an article.
   *
   * @param articleId the ID of the article whose tags should be removed
   * @param tags the list of tag strings to remove
   * @return Unit on successful deletion
   */
  override def delete(articleId: Article.Id, tags: List[String]): Unit < Effect =
    if tags.isEmpty then ()
    else
      Transactional {
        sql"""DELETE FROM tags
              WHERE article_id = $articleId
                AND name = ANY($tags)"""
          .update
          .run()
      }.unit
