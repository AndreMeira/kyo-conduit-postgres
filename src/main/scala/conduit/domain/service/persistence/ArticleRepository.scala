package conduit.domain.service.persistence

import conduit.domain.error.ApplicationError
import conduit.domain.model.{Article, User}
import conduit.domain.service.persistence.ArticleRepository.SearchParam
import conduit.domain.service.persistence.Database.Transaction
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
  def search(params: List[SearchParam]): List[Article] < Effect
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
