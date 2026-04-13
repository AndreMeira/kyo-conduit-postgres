package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.error.NotFound.ArticleNotFound
import conduit.domain.model.Article.Favorite
import conduit.domain.model.UserProfile.Follower
import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.request.article.GetArticleRequest
import conduit.domain.response.article.GetArticleResponse
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.Database.Transaction
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for reading an article by its slug.
 *
 * This use case handles retrieving a single article along with its author's profile,
 * and determining whether the requester has favorited the article or is following the author.
 * The operation is executed within a database transaction.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @param authentication The authentication service for handling user authentication.
 * @tparam Tx The type of database transaction.
 */
class ArticleReadUseCase[Tx <: Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
  authentication: AuthenticationService,
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Retrieves an article by its slug with associated metadata.
   *
   * Executes within a database transaction to:
   * 1. Find the article by slug
   * 2. Find the author's profile
   * 3. Check if the requester has favorited the article
   * 4. Check if the requester is following the author
   *
   * @param request The request containing the article slug and optional requester.
   * @return The article response with metadata wrapped in Effect context.
   */
  def apply(request: GetArticleRequest): GetArticleResponse < Effect =
    database.transaction:
      for {
        article    <- findArticle(request)
        profile    <- findProfile(article)
        isFavorite <- isFavorite(request.requester, article)
        isFollowed <- isFollowing(request.requester, profile)
      } yield GetArticleResponse.make(article, profile, isFavorite, isFollowed)

  /**
   * Finds an article by its slug.
   *
   * @param request The request containing the article slug.
   * @return The article if found, or aborts with ArticleNotFound error.
   */
  private def findArticle(request: GetArticleRequest): Article < (Effect & Env[Tx]) =
    persistence.articles.findBySlug(request.slug) ?! ArticleNotFound(request.slug)

  /**
   * Finds the user profile of the article's author.
   *
   * @param article The article whose author profile should be retrieved.
   * @return The author's profile if found, or aborts with UserProfileMissing error.
   */
  private def findProfile(article: Article): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUser(article.authorId) ?! UserProfileMissing(article.authorId)

  /**
   * Checks if the requester is following the article's author.
   *
   * Returns false if the requester is not authenticated.
   *
   * @param requester The user making the request (authenticated or anonymous).
   * @param profile The author's profile to check following status for.
   * @return True if the requester is following the author, false otherwise.
   */
  private def isFollowing(requester: User, profile: UserProfile): Boolean < (Effect & Env[Tx]) =
    requester.option match
      case Maybe.Present(userId) => persistence.followers.exists(Follower(userId, profile.id))
      case _                     => false

  /**
   * Checks if the requester has favorited the article.
   *
   * Returns false if the requester is not authenticated.
   *
   * @param requester The user making the request (authenticated or anonymous).
   * @param article The article to check favorite status for.
   * @return True if the requester has favorited the article, false otherwise.
   */
  private def isFavorite(requester: User, article: Article): Boolean < (Effect & Env[Tx]) =
    requester.option match
      case Maybe.Present(userId) => persistence.favorites.exists(Favorite(userId, article.id))
      case _                     => false
}
