package conduit.domain.service.usecase

import conduit.domain.service.persistence.{Database, Persistence}
import conduit.domain.model.{Article, User, UserProfile}
import conduit.domain.error.ApplicationError
import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.error.NotFound.ArticleNotFound
import conduit.domain.request.article.RemoveFavoriteArticleRequest
import conduit.domain.response.article.GetArticleResponse
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for unfavoriting an article.
 *
 * This use case handles the action of a user removing an article from their favorites.
 * It performs the operation within a database transaction, ensuring the article and author exist,
 * removes the favorite relationship, and returns the article response with the updated favorited status.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @tparam Tx The type of database transaction.
 */
class ArticleUnfavoriteUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  type Effect = Async & Abort[ApplicationError]

  /**
   * Unfavorites an article for the requesting user.
   *
   * Executes within a database transaction to:
   * 1. Find the article by slug
   * 2. Find the author's profile
   * 3. Check if the requester is following the author
   * 4. Remove the favorite relationship
   * 5. Return the article response with favorited flag set to false
   *
   * @param request The request containing the requester and article slug.
   * @return The article response with updated favorited status wrapped in Effect context.
   */
  def apply(request: RemoveFavoriteArticleRequest): GetArticleResponse < Effect =
    database.transaction:
      for {
        article   <- findArticle(request)
        profile   <- findAuthor(article)
        following <- isFollowing(request.requester.userId, profile)
        favorite   = Article.FavoriteBy(request.requester.userId, article.id)
        _         <- persistence.favorites.delete(favorite)
      } yield GetArticleResponse.make(article, profile, favorited = false, following)

  /**
   * Finds an article by its slug.
   *
   * @param request The request containing the article slug.
   * @return The article if found, or aborts with ArticleNotFound error.
   */
  def findArticle(request: RemoveFavoriteArticleRequest): Article < (Effect & Env[Tx]) =
    persistence.articles.findBySlug(request.slug) ?! ArticleNotFound(request.slug)

  /**
   * Finds the user profile of the article's author.
   *
   * @param article The article whose author profile should be retrieved.
   * @return The author's profile if found, or aborts with UserProfileMissing error.
   */
  def findAuthor(article: Article): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUser(article.authorId) ?! UserProfileMissing(article.authorId)

  /**
   * Checks if the requester is following the article's author.
   *
   * @param userId The ID of the requesting user.
   * @param profile The author's user profile.
   * @return True if the requester is following the author, false otherwise.
   */
  def isFollowing(userId: User.Id, profile: conduit.domain.model.UserProfile): Boolean < (Effect & Env[Tx]) =
    persistence.followers.exists(UserProfile.FollowedBy(userId, profile.id))
}
