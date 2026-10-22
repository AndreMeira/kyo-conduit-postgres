package conduit.infrastructure.postgres

import conduit.domain.model.Article
import conduit.domain.service.persistence.TagRepository
import conduit.domain.types.TagName
import conduit.infrastructure.codecs.database.SqlSchemaCodecs.given
import PostgresTransaction.Transactional
import kyo.*

/**
 * Postgres implementation of [[TagRepository]], over the `tags` table.
 */
class PostgresTagRepository extends TagRepository[PostgresTransaction]:

  /**
   * Retrieves every distinct tag, ordered alphabetically.
   *
   * @return the tags; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def findAll: List[String] < Effect =
    Transactional:
      Sql.from[TagRow]("t", "tags").select(_.t.name).distinct.orderBy(_.t.name.asc).run.map(_.toList)

  /**
   * Attaches tags to an article in one statement; a tag it already carries is left as it is.
   *
   * @param articleId the article to tag
   * @param tags the tags to attach
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the insert fails
   */
  override def add(articleId: Article.Id, tags: List[String]): Unit < Effect =
    if tags.isEmpty then Kyo.unit
    else
      Transactional:
        sql"""INSERT INTO tags (name, article_id)
              SELECT unnest(${Chunk.from(tags)}), $articleId
              ON CONFLICT (name, article_id) DO NOTHING""".execute.unit

  /**
   * Retrieves the tags attached to an article.
   *
   * @param articleId the article
   * @return its tags; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def find(articleId: Article.Id): List[String] < Effect =
    Transactional:
      Sql.from[TagRow]("t", "tags").where(_.t.articleId == articleId).select(_.t.name).run.map(_.toList)

  /**
   * Detaches tags from an article; a tag it does not carry is ignored.
   *
   * @param articleId the article
   * @param tags the tags to detach
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the delete fails
   */
  override def delete(articleId: Article.Id, tags: List[String]): Unit < Effect =
    if tags.isEmpty then Kyo.unit
    else
      Transactional:
        Sql.delete[TagRow]("tags")
          .where(row => row.articleId == articleId && row.name.in(tags.map(TagName(_))*))
          .run
          .unit
