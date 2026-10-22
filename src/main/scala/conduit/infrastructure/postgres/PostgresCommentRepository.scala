package conduit.infrastructure.postgres

import conduit.domain.model.{ Article, Comment }
import conduit.domain.service.persistence.CommentRepository
import conduit.domain.types.CommentId
import conduit.infrastructure.codecs.database.SqlSchemaCodecs.given
import PostgresTransaction.Transactional
import kyo.*

/**
 * Postgres implementation of [[CommentRepository]], over the `comments` table. Comment ids are
 * assigned by the database.
 */
class PostgresCommentRepository extends CommentRepository[PostgresTransaction]:

  /**
   * Finds a comment by its id.
   *
   * @param id the comment
   * @return the comment, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def find(id: Comment.Id): Maybe[Comment] < Effect =
    Transactional:
      Sql.from[Comment]("c", "comments").where(_.c.id == id).run.map(rows => Maybe.fromOption(rows.headOption))

  /**
   * Whether a comment exists.
   *
   * @param id the comment
   * @return true when it exists; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def exists(id: Comment.Id): Boolean < Effect =
    Transactional:
      Sql.from[Comment]("c", "comments").where(_.c.id == id).limit(1).run.map(_.nonEmpty)

  /**
   * Saves a new comment, letting the database assign its id.
   *
   * @param comment the comment to save
   * @return the saved comment, carrying its id; aborts with `PostgresDatabase.Error.QueryError` when the
   *         insert fails, or with `PostgresDatabase.Error.UnexpectedRowCount` when it returns no row
   */
  override def save(comment: Comment.Data): Comment < Effect =
    Transactional(insert(comment)).map(stored)

  /**
   * Updates a comment's body and update time.
   *
   * @param comment the comment, carrying its new body
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the update fails, or with
   *         `PostgresDatabase.Error.UnexpectedRowCount` when no comment has its id
   */
  override def update(comment: Comment): Unit < Effect =
    Transactional.single("comments"):
      Sql.update[Comment]("comments")
        .set(_.body := comment.body, _.updatedAt := comment.updatedAt)
        .where(_.id == comment.id)
        .run

  /**
   * Retrieves the comments on an article, oldest first.
   *
   * @param articleId the article
   * @return its comments; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def findByArticleId(articleId: Article.Id): List[Comment] < Effect =
    Transactional:
      Sql.from[Comment]("c", "comments").where(_.c.articleId == articleId).orderBy(_.c.createdAt.asc).run.map(_.toList)

  /**
   * Deletes a comment; one that does not exist is ignored.
   *
   * @param id the comment
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the delete fails
   */
  override def delete(id: Comment.Id): Unit < Effect =
    Transactional:
      Sql.delete[Comment]("comments").where(_.id == id).run.unit

  /**
   * Inserts a comment with a database-assigned id, returning the stored row.
   *
   * @param comment the comment to insert
   * @return the stored row; aborts with `SqlException` when the insert fails
   */
  private def insert(comment: Comment.Data): Chunk[Comment] < (DB & Abort[SqlException]) =
    Sql.insert[Comment]("comments")
      .values(Comment(CommentId(0L), comment.articleId, comment.body, comment.authorId, comment.createdAt, comment.updatedAt))
      .overriding(_.id := Sql.default)
      .returning(row => (row.id, row.articleId, row.body, row.authorId, row.createdAt, row.updatedAt))
      .run
      .map(_.map(Comment.apply.tupled))

  /**
   * The comment an insert returned.
   *
   * @param rows the rows the insert returned
   * @return the first one; aborts with `PostgresDatabase.Error.UnexpectedRowCount` when there is none
   */
  private def stored(rows: Chunk[Comment]): Comment < Abort[PostgresDatabase.Error] =
    rows.headOption match
      case Some(comment) => comment
      case None          => Abort.fail(PostgresDatabase.Error.UnexpectedRowCount("comments", 0))
