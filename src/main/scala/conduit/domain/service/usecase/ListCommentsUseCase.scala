package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.error.NotFound.ArticleNotFound
import conduit.domain.model.{ Article, Comment, User, UserProfile }
import conduit.domain.request.comment.ListCommentsRequest
import conduit.domain.response.comment.CommentListResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for listing the comments attached to an article.
 *
 * The requester may be anonymous — in that case no author is reported as
 * followed. The target article is looked up by slug so an informative
 * [[ArticleNotFound]] error is returned when the slug does not exist.
 *
 * @param database    The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @tparam Tx the database transaction type.
 */
class ListCommentsUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Lists the comments of the article identified by the request slug.
   *
   * @param request The list comments request.
   * @return A response with all comments and their author metadata.
   */
  def apply(request: ListCommentsRequest): CommentListResponse < Effect =
    database.transaction:
      for {
        article  <- findArticle(request.slug)
        comments <- persistence.comments.findByArticleId(article.id)
        profiles <- findAuthors(comments)
        followed <- followedBy(request.requester, profiles)
      } yield CommentListResponse.make(comments, profiles, followed)

  /**
   * Finds the target article by slug.
   *
   * @param slug The article slug.
   * @return The article, or aborts with [[ArticleNotFound]] if missing.
   */
  private def findArticle(slug: String): Article < (Effect & Env[Tx]) =
    persistence.articles.findBySlug(slug) ?! ArticleNotFound(slug)

  /**
   * Finds the profiles of the distinct authors of the given comments.
   *
   * @param comments The comments whose authors should be resolved.
   * @return The matching user profiles.
   */
  private def findAuthors(comments: List[Comment]): List[UserProfile] < (Effect & Env[Tx]) =
    persistence.users.findByUsers(comments.map(_.authorId).distinct)

  /**
   * Determines which of the given author profiles are followed by the
   * requester. Anonymous requesters never follow anyone.
   *
   * @param requester The requesting user (possibly anonymous).
   * @param authors   The author profiles to check.
   * @return The set of author profile ids that the requester follows.
   */
  private def followedBy(requester: User, authors: List[UserProfile]): Set[UserProfile.Id] < (Effect & Env[Tx]) =
    requester match {
      case User.Anonymous          => Set.empty[UserProfile.Id]
      case User.Authenticated(uid) => persistence.followers.followedBy(uid, authors.map(_.id)).map(_.toSet)
    }
}
