package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.error.NotFound.ArticleNotFound
import conduit.domain.model.{ Article, Comment, User, UserProfile }
import conduit.domain.request.comment.AddCommentRequest
import conduit.domain.response.comment.GetCommentResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.service.validation.CommentInputValidation
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for adding a new comment to an article.
 *
 * Only authenticated users can add comments. The use case looks up the
 * target article by slug, validates the comment body, persists the comment
 * within a transaction and returns the newly created comment together with
 * the author's profile.
 *
 * @param database    The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @tparam Tx the database transaction type.
 */
class CommentAdditionUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Adds a comment to an article identified by the request slug.
   *
   * @param request The authenticated request containing the article slug and
   *                comment payload.
   * @return The created comment response.
   */
  def apply(request: AddCommentRequest): GetCommentResponse < Effect =
    database.transaction:
      for {
        article <- findArticle(request.slug)
        profile <- findProfile(request.requester)
        data    <- parse(request, article).validOrAbort
        comment <- persistence.comments.save(data)
      } yield GetCommentResponse.make(comment, profile, following = false)

  /**
   * Validates the request payload and builds a [[Comment.Data]] ready for
   * persistence. The comment is timestamped with the current clock.
   *
   * @param request The add comment request.
   * @param article The article the comment is being attached to.
   * @return A validated [[Comment.Data]].
   */
  private def parse(request: AddCommentRequest, article: Article): Validated[Comment.Data] < Effect =
    Clock.now.map(_.toJava).map { now =>
      CommentInputValidation
        .body(request.payload.comment.body)
        .map(body => Comment.Data(article.id, body, request.requester.userId, now, now))
    }

  /**
   * Finds the target article by slug.
   *
   * @param slug The article slug.
   * @return The article, or aborts with [[ArticleNotFound]] if missing.
   */
  private def findArticle(slug: String): Article < (Effect & Env[Tx]) =
    persistence.articles.findBySlug(slug) ?! ArticleNotFound(slug)

  /**
   * Finds the requester's profile.
   *
   * @param user The authenticated requester.
   * @return The user's profile, or aborts with [[UserProfileMissing]].
   */
  private def findProfile(user: User.Authenticated): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUser(user.userId) ?! UserProfileMissing(user.userId)
}
