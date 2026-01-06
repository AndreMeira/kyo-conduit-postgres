package conduit.domain.service.persistence

import conduit.domain.error.ApplicationError
import conduit.domain.model.{Article, Comment}
import conduit.domain.service.persistence.Database.Transaction
import kyo.*

/**
 * Repository trait for managing comment persistence operations.
 *
 * This trait defines the contract for comment storage and retrieval,
 * allowing different implementations (in-memory, database, etc.) to provide
 * persistent storage for comments in the Conduit application.
 *
 * @tparam Tx the transaction type used for database operations
 */
trait CommentRepository[Tx <: Transaction] {
  type Effect = Async & Abort[ApplicationError] & Env[Tx]

  /**
   * Finds a comment by its unique ID.
   *
   * @param id the comment ID to search for
   * @return a Maybe containing the comment if found, or None if not found
   */
  def find(id: Comment.Id): Maybe[Comment] < Effect

  /**
   * Checks if a comment with the given ID exists in the repository.
   *
   * @param id the comment ID to check
   * @return true if the comment exists, false otherwise
   */
  def exists(id: Comment.Id): Boolean < Effect

  /**
   * Saves a new comment to the repository.
   *
   * @param comment the comment to save
   * @return Unit on successful save
   */
  def save(comment: Comment): Unit < Effect

  /**
   * Updates an existing comment in the repository.
   *
   * @param comment the comment with updated data
   * @return Unit on successful update
   */
  def update(comment: Comment): Unit < Effect

  /**
   * Finds all comments for a specific article.
   *
   * @param articleId the article ID to retrieve comments for
   * @return a list of comments for the specified article
   */
  def findByArticleId(articleId: Article.Id): List[Comment] < Effect

  /**
   * Deletes a comment from the repository.
   *
   * @param id the comment ID to delete
   * @return Unit on successful deletion
   */
  def delete(id: Comment.Id): Unit < Effect
}
