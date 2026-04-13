package conduit.domain.service.usecase

import conduit.domain.error.{ ApplicationError, Unauthorised }
import conduit.domain.error.NotFound.ArticleNotFound
import conduit.domain.error.Unauthorised.ArticleDeleteDenied
import conduit.domain.model.{ Article, User }
import conduit.domain.request.article.DeleteArticleRequest
import conduit.domain.response.article.DeleteArticleResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.syntax.*
import kyo.*

/** Use case for deleting an article.
  *
  * Only the original author of the article may delete it. The article is looked
  * up by slug, the requester's identity is checked against the article author,
  * and then the article, its tags, its comments, and its favorites are removed.
  *
  * @param database    the database abstraction for managing transactions
  * @param persistence the persistence layer providing access to repositories
  * @tparam Tx the database transaction type
  */
class ArticleDeletionUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /** Deletes the article described by the request.
    *
    * @param request the authenticated delete article request
    * @return a response carrying the slug of the deleted article
    */
  def apply(request: DeleteArticleRequest): DeleteArticleResponse < Effect =
    database.transaction:
      for
        article <- findArticle(request.slug)
        _       <- authorise(request.requester, article)
        _       <- persistence.tags.delete(article.id, article.tags)
        _       <- persistence.articles.delete(article.id)
      yield DeleteArticleResponse(article.slug)

  /** Ensures that the requester owns the given article.
    *
    * @param requester the authenticated requester
    * @param article   the article being deleted
    * @return Unit if authorised, otherwise aborts with [[ArticleDeleteDenied]]
    */
  private def authorise(requester: User.Authenticated, article: Article): Unit < Abort[Unauthorised] =
    if article.authorId == requester.userId then ()
    else Abort.fail(ArticleDeleteDenied)

  /** Finds the target article by slug.
    *
    * @param slug the article slug
    * @return the article, or aborts with [[ArticleNotFound]] if missing
    */
  private def findArticle(slug: String): Article < (Effect & Env[Tx]) =
    persistence.articles.findBySlug(slug) ?! ArticleNotFound(slug)
}
