package conduit.domain.service.usecase

import conduit.domain.error.{ ApplicationError, Unauthorised }
import conduit.domain.error.NotFound.{ ArticleNotFound, CommentNotFound }
import conduit.domain.error.Unauthorised.CommentDeleteDenied
import conduit.domain.model.{ Article, Comment, User }
import conduit.domain.request.comment.DeleteCommentRequest
import conduit.domain.response.comment.DeleteCommentResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for deleting a comment from an article.
 *
 * Only the original author of the comment may delete it. The target article
 * is looked up by slug to ensure it exists, the comment is fetched by id and
 * the requester's identity is checked against the comment author before the
 * deletion is performed.
 *
 * @param database    The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @tparam Tx the database transaction type.
 */
class CommentDeletionUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Deletes the comment described by the request.
   *
   * @param request The authenticated delete comment request.
   * @return A response carrying the id of the deleted comment.
   */
  def apply(request: DeleteCommentRequest): DeleteCommentResponse < Effect =
    database.transaction:
      for {
        _       <- findArticle(request.slug)
        comment <- findComment(request.commentId)
        _       <- authorise(request.requester, comment)
        _       <- persistence.comments.delete(comment.id)
      } yield DeleteCommentResponse(comment.id)

  /**
   * Ensures that the requester owns the given comment.
   *
   * @param requester The authenticated requester.
   * @param comment   The comment being deleted.
   * @return Unit if authorised, otherwise aborts with [[CommentDeleteDenied]].
   */
  private def authorise(requester: User.Authenticated, comment: Comment): Unit < Abort[Unauthorised] =
    if comment.authorId == requester.userId then ()
    else Abort.fail(CommentDeleteDenied)

  /**
   * Finds the target article by slug.
   *
   * @param slug The article slug.
   * @return The article, or aborts with [[ArticleNotFound]] if missing.
   */
  private def findArticle(slug: String): Article < (Effect & Env[Tx]) =
    persistence.articles.findBySlug(slug) ?! ArticleNotFound(slug)

  /**
   * Finds the comment by id.
   *
   * @param id The comment id.
   * @return The comment, or aborts with [[CommentNotFound]] if missing.
   */
  private def findComment(id: Comment.Id): Comment < (Effect & Env[Tx]) =
    persistence.comments.find(id) ?! CommentNotFound(id)
}
