package conduit.infrastructure.postgres

import com.augustnagro.magnum.*
import conduit.domain.model.{ Article, Comment }
import conduit.domain.service.persistence.CommentRepository
import conduit.infrastructure.codecs.database.DatabaseCodecs.given
import conduit.infrastructure.postgres.PostgresTransaction.Transactional
import kyo.*

class PostgresCommentRepository extends CommentRepository[PostgresTransaction]:

  /**
   * Finds a comment by its unique ID.
   *
   * @param id the comment ID to search for
   * @return a Maybe containing the comment if found, or None if not found
   */
  override def find(id: Comment.Id): Maybe[Comment] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT c.id, c.article_id, c.body, c.author_id, c.created_at, c.updated_at
              FROM comments c
              WHERE c.id = $id"""
          .query[Comment]
          .run()
          .headOption

  /**
   * Checks if a comment with the given ID exists in the repository.
   *
   * @param id the comment ID to check
   * @return true if the comment exists, false otherwise
   */
  override def exists(id: Comment.Id): Boolean < Effect =
    Transactional:
      sql"""SELECT EXISTS(SELECT 1 FROM comments WHERE id = $id)"""
        .query[Boolean]
        .run()
        .headOption.contains(true)

  /**
   * Saves a new comment to the repository and returns the persisted comment with
   * its database-generated ID.
   *
   * The comment ID is assigned by the database (GENERATED ALWAYS AS IDENTITY).
   * RETURNING is used to retrieve the full row immediately after insert.
   *
   * @param data the comment data to persist
   * @return the saved comment with its DB-generated ID
   */
  override def save(data: Comment.Data): Comment < Effect =
    Transactional:
      sql"""INSERT INTO comments (body, author_id, article_id, created_at, updated_at)
            VALUES (
              ${data.body},
              ${data.authorId},
              ${data.articleId},
              ${data.createdAt},
              ${data.updatedAt}
            )
            RETURNING id, article_id, body, author_id, created_at, updated_at"""
        .query[Comment]
        .run()
        .head

  /**
   * Updates an existing comment in the repository.
   *
   * @param comment the comment with updated data
   * @return Unit on successful update
   */
  override def update(comment: Comment): Unit < Effect =
    Transactional:
      val count = sql"""
          UPDATE comments SET
            body       = ${comment.body},
            updated_at = ${comment.updatedAt}
          WHERE id = ${comment.id}"""
        .update
        .run()
      require(count == 1, "Failed to update comment")

  /**
   * Finds all comments for a specific article, ordered by creation date.
   *
   * @param articleId the article ID to retrieve comments for
   * @return a list of comments for the specified article
   */
  override def findByArticleId(articleId: Article.Id): List[Comment] < Effect =
    Transactional:
      sql"""SELECT c.id, c.article_id, c.body, c.author_id, c.created_at, c.updated_at
            FROM comments c
            WHERE c.article_id = $articleId
            ORDER BY c.created_at ASC"""
        .query[Comment]
        .run()
        .toList

  /**
   * Deletes a comment from the repository.
   *
   * @param id the comment ID to delete
   * @return Unit on successful deletion
   */
  override def delete(id: Comment.Id): Unit < Effect =
    Transactional {
      sql"""DELETE FROM comments WHERE id = $id"""
        .update
        .run()
    }.unit
        
