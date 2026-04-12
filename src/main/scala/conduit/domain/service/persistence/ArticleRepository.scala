package conduit.domain.service.persistence

import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Article, User }
import ArticleRepository.SearchParam
import Database.Transaction
import kyo.*

/**
 * Repository trait for managing article persistence operations.
 *
 * This trait defines the contract for article storage and retrieval,
 * allowing different implementations (in-memory, database, etc.) to provide
 * persistent storage for articles in the Conduit application.
 *
 * @tparam Tx the transaction type used for database operations
 */
trait ArticleRepository[Tx <: Transaction] {
  type Effect = Async & Abort[ApplicationError] & Env[Tx]

  /**
   * Finds an article by its unique ID.
   *
   * @param id the article ID to search for
   * @return a Maybe containing the article if found, or None if not found
   */
  def find(id: Article.Id): Maybe[Article] < Effect

  /**
   * Finds an article by its slug.
   * 
   * @param slug the article slug
   * @return a Maybe containing the article if found, or None if not found
   */
  def findBySlug(slug: String): Maybe[Article] < Effect

  /**
   * Checks if an article with the given ID exists in the repository.
   *
   * @param id the article ID to check
   * @return true if the article exists, false otherwise
   */
  def exists(id: Article.Id): Boolean < Effect

  /**
   * Saves a new article to the repository.
   *
   * @param article the article to save
   * @return Unit on successful save
   */
  def save(article: Article): Unit < Effect

  /**
   * Updates an existing article in the repository.
   *
   * @param article the article with updated data
   * @return Unit on successful update
   */
  def update(article: Article): Unit < Effect

  /**
   * Searches for articles based on multiple search parameters.
   *
   * Supports filtering by tag, author, and favorite user. Multiple parameters
   * are applied as cumulative filters (AND logic).
   *
   * @param params a list of search parameters to apply
   * @return a list of articles matching all search criteria
   */
  def search(params: List[SearchParam], offset: Int, limit: Int): List[Article] < Effect

  /**
   * Counts the total number of articles matching the given search parameters.
   *
   * Uses the same filtering semantics as [[searchBy]], ignoring pagination.
   *
   * @param params a list of search parameters to apply
   * @return the total number of articles matching all search criteria
   */
  def searchCount(params: List[SearchParam]): Int < Effect

  /**
   * Retrieves a feed of articles for a specific user.
   *
   * The feed consists of articles from authors that the user follows.
   *
   * @param userId the ID of the user for whom to retrieve the feed
   * @param offset the starting index for pagination
   * @param limit the maximum number of articles to return
   * @return a list of articles in the user's feed
   */
  def feedOf(userId: User.Id, offset: Int, limit: Int): List[Article] < Effect

  /**
   * Counts the total number of articles in a user's feed.
   *
   * @param userId the ID of the user whose feed articles to count
   * @return the total count of articles in the user's feed
   */
  def countFeedOf(userId: User.Id): Int < Effect
}

object ArticleRepository:
  /**
   * Sealed enum representing different search filter parameters for articles.
   */
  enum SearchParam:
    /**
     * Filter articles by a specific tag.
     *
     * @param value the tag name to filter by
     */
    case Tag(value: String)

    /**
     * Filter articles by author username.
     *
     * @param username the author's username
     */
    case Author(username: String)

    /**
     * Filter articles that are favorited by a specific user.
     *
     * @param username the username of the user whose favorites to search
     */
    case FavoriteBy(username: String)
