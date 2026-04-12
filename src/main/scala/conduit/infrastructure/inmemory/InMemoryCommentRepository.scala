package conduit.infrastructure.inmemory

import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Article, Comment }
import conduit.domain.service.persistence.CommentRepository
import conduit.infrastructure.inmemory.InMemoryState.Changed.{ Deleted, Inserted, Updated }
import conduit.infrastructure.inmemory.InMemoryState.RowReference.CommentRow
import kyo.*

/**
 * In-memory implementation of the CommentRepository for the Conduit application.
 *
 * This repository stores comments in memory and provides operations for finding, saving,
 * updating, and deleting comments. All operations are wrapped in InMemoryTransaction
 * to ensure consistent access to the shared comment state.
 */
class InMemoryCommentRepository(lock: Meter) extends CommentRepository[InMemoryTransaction] {

  /**
   * Finds a comment by its ID.
   *
   * @param id the comment ID to search for
   * @return a Maybe containing the comment if found, or None otherwise
   */
  override def find(id: Comment.Id): Maybe[Comment] < Effect =
    InMemoryTransaction { state =>
      state.comments.get.map(_.get(id)).map(Maybe.fromOption)
    }

  /**
   * Checks if a comment with the given ID exists.
   *
   * @param id the comment ID to check
   * @return true if the comment exists, false otherwise
   */
  override def exists(id: Comment.Id): Boolean < Effect =
    InMemoryTransaction { state =>
      state.comments.get.map(_.contains(id))
    }

  /**
   * Saves a new comment to the repository and returns the persisted comment.
   *
   * The in-memory implementation uses comment.id as-is, mirroring the behaviour
   * callers expect after a save (the returned comment carries the assigned ID).
   *
   * @param comment the comment to save
   * @return the saved comment
   */
  override def save(comment: Comment.Data): Comment < Effect = Scope.run {
    lock
      .run {
        InMemoryTransaction { state =>
          nextCommentId(state).map: id =>
            state.addChange(Inserted(CommentRow(id)))
              *> state.comments.updateAndGet(_ + (id -> comment.withId(id)))
              *> comment.withId(id)
        }
      }
      .mapAbort(_ => InMemoryCommentRepository.LockError)
  }

  /**
   * Updates an existing comment in the repository.
   *
   * @param comment the comment to update
   * @return Unit
   */
  override def update(comment: Comment): Unit < Effect =
    InMemoryTransaction { state =>
      state.addChange(Updated(CommentRow(comment.id)))
        *> state.comments.updateAndGet(_ + (comment.id -> comment)).unit
    }

  /**
   * Finds all comments for a specific article.
   *
   * @param articleId the article ID to retrieve comments for
   * @return a list of comments associated with the specified article
   */
  override def findByArticleId(articleId: Article.Id): List[Comment] < Effect =
    InMemoryTransaction { state =>
      for {
        commentsByArticleId <- state.commentsByArticleId
      } yield commentsByArticleId.getOrElse(articleId, Nil).sortBy(_.id)
    }

  /**
   * Deletes a comment from the repository.
   *
   * @param id the comment ID to delete
   * @return Unit
   */
  override def delete(id: Comment.Id): Unit < Effect =
    InMemoryTransaction { state =>
      state.addChange(Deleted(CommentRow(id)))
        *> state.comments.updateAndGet(_ - id).unit
    }

  /**
   * Give the next comment id
   * This is concurrent safe, so it should be used inside a lock
   * 
   * @return Long
   */
  private def nextCommentId(state: InMemoryState): Long < Effect =
    state
      .comments
      .get
      .map: comments =>
        comments.keySet.toList.sorted.lastOption.map(_ + 1L).getOrElse(1L)
}

object InMemoryCommentRepository {
  case object LockError extends ApplicationError.TransientError:
    override def message: String = "Can not acquire lock"
}
