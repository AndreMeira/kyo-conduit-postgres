package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.request.article.ArticleFeedRequest
import conduit.domain.response.article.ArticleListResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import kyo.*

/**
 * Use case for fetching a feed of articles for a user.
 * Handles fetching articles, authors, favorites, and followed profiles.
 *
 * @param database Database service for transaction management.
 * @param persistence Persistence service for accessing entities.
 * @tparam Tx Database transaction type.
 */
class ArticleFeedUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Fetches a feed of articles for the requester.
   *
   * Executes within a database transaction to:
   * 1. Count the total number of articles in the feed
   * 2. Fetch the list of articles based on pagination parameters
   * 3. Fetch the profiles of the authors of the articles
   * 4. Fetch the list of article IDs favorited by the requester
   * 5. Fetch the list of author IDs followed by the requester
   * 6. Combine all data into an ArticleListResponse
   *
   * @param request The article feed request containing pagination and requester info.
   * @return An ArticleListResponse containing the feed data wrapped in Effect context.
   */
  def apply(request: ArticleFeedRequest): ArticleListResponse < Effect =
    database.transaction:
      for {
        count     <- count(request)
        articles  <- findArticles(request)
        profiles  <- findAuthors(articles)
        favorites <- favorites(request.requester.userId, articles)
        followed  <- followed(request.requester.userId, profiles)
      } yield ArticleListResponse.make(count, articles, profiles, favorites.toSet, followed.toSet)

  /**
   * Counts the total number of articles in the feed for the requester.
   *
   * @param request The article feed request containing requester info.
   * @return The total count of articles in the feed wrapped in Effect context.
   */
  private def count(request: ArticleFeedRequest): Int < (Effect & Env[Tx]) =
    persistence.articles.countFeedOf(request.requester.userId)

  /**
   * Fetches a list of articles for the feed based on pagination parameters.
   *
   * @param request The article feed request containing pagination and requester info.
   * @return A list of articles for the feed wrapped in Effect context.
   */
  private def findArticles(request: ArticleFeedRequest): List[Article] < (Effect & Env[Tx]) =
    persistence.articles.feedOf(request.requester.userId, request.offset, request.limit)

  /**
   * Fetches the profiles of the authors of the given list of articles.
   * 
   * @param articles The list of articles for which to fetch author profiles.
   * @return A list of user profiles corresponding to the authors of the articles wrapped in Effect context.
   */
  private def findAuthors(articles: List[Article]): List[UserProfile] < (Effect & Env[Tx]) =
    persistence.users.findByUsers(articles.map(_.authorId))

  /**
   * Fetches the list of article IDs that are favorited by the requester for the given list of articles.
   * 
   * @param userId The ID of the requester for whom to fetch favorited articles.
   * @param articles The list of articles to check for favorites.
   * @return A list of article IDs that are favorited by the requester wrapped in Effect context.
   */
  private def favorites(userId: User.Id, articles: List[Article]): List[Article.Id] < (Effect & Env[Tx]) =
    persistence.favorites.favoriteOf(userId, articles.map(_.id))

  /**
   * Fetches the list of author IDs that are followed by the requester for the given list of author profiles.
   * 
   * @param userId The ID of the requester for whom to fetch followed authors.
   * @param authors The list of author profiles to check for followed status.
   * @return A list of author IDs that are followed by the requester wrapped in Effect context.
   */
  private def followed(userId: User.Id, authors: List[UserProfile]): List[UserProfile.Id] < (Effect & Env[Tx]) =
    persistence.followers.followedBy(userId, authors.map(_.id))
}
