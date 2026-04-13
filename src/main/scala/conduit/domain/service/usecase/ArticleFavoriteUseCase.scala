package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.error.NotFound.ArticleNotFound
import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.request.article.AddFavoriteArticleRequest
import conduit.domain.response.article.GetArticleResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for favoriting an article.
 *
 * This use case handles the action of a user adding an article to their favorites.
 * It performs the operation within a database transaction, ensuring the article and author exist,
 * adds the favorite relationship, and returns the article response with the updated favorited status.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @tparam Tx The type of database transaction.
 */
class ArticleFavoriteUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Favorites an article for the requesting user.
   *
   * Executes within a database transaction to:
   * 1. Find the article by slug
   * 2. Find the author's profile
   * 3. Check if the requester is following the author
   * 4. Add the favorite relationship
   * 5. Return the article response with favorited flag set to true
   *
   * @param request The request containing the requester and article slug.
   * @return The article response with updated favorited status wrapped in Effect context.
   */
  def apply(request: AddFavoriteArticleRequest): GetArticleResponse < Effect =
    database.transaction:
      for {
        article   <- findArticle(request)
        profile   <- findAuthor(article)
        following <- isFollowing(request.requester.userId, profile)
        favorite   = Article.Favorite(request.requester.userId, article.id)
        _         <- persistence.favorites.add(favorite)
        updated   <- findArticle(request)
      } yield GetArticleResponse.make(updated, profile, favorited = true, following)

  /**
   * Finds an article by its slug.
   *
   * @param request The request containing the article slug.
   * @return The article if found, or aborts with ArticleNotFound error.
   */
  private def findArticle(request: AddFavoriteArticleRequest): Article < (Effect & Env[Tx]) =
    persistence.articles.findBySlug(request.slug) ?! ArticleNotFound(request.slug)

  /**
   * Finds the user profile of the article's author.
   *
   * @param article The article whose author profile should be retrieved.
   * @return The author's profile if found, or aborts with UserProfileMissing error.
   */
  private def findAuthor(article: Article): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUser(article.authorId) ?! UserProfileMissing(article.authorId)

  /**
   * Checks if the requester is following the article's author.
   *
   * @param userId The ID of the requesting user.
   * @param profile The author's user profile.
   * @return True if the requester is following the author, false otherwise.
   */
  private def isFollowing(userId: User.Id, profile: UserProfile): Boolean < (Effect & Env[Tx]) =
    persistence.followers.exists(UserProfile.Follower(userId, profile.id))
}
