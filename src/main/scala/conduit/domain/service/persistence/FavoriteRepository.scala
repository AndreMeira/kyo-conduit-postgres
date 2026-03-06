package conduit.domain.service.persistence

import conduit.domain.model.*
import conduit.domain.error.ApplicationError
import conduit.domain.service.persistence.Database.Transaction
import kyo.*

/**
 * Repository trait for managing article favorite persistence operations.
 *
 * This trait defines the contract for storing and removing article favorites,
 * allowing different implementations (in-memory, database, etc.) to provide
 * persistent storage for user favorites in the Conduit application.
 *
 * @tparam Tx the transaction type used for database operations
 */
trait FavoriteRepository[Tx <: Transaction] {
  type Effect = Async & Abort[ApplicationError] & Env[Tx]
  
  /**
   * Checks if a favorite entry for an article by a user exists.
   *
   * @param favorite the favorite entry to check
   * @return true if the favorite entry exists, false otherwise
   */
  def exists(favorite: Article.FavoriteBy): Boolean < Effect
  
  /**
   * Finds favorite entries for a user across multiple articles.
   *
   * @param userId the ID of the user
   * @param articleIds the list of article IDs to check
   * @return List of favorite entries found
   */
  def favoriteOf(userId: User.Id, articleIds: List[Article.Id]): List[Article.Id] < Effect

  /**
   * Adds a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to add
   * @return Unit on successful addition
   */
  def add(favorite: Article.FavoriteBy): Unit < Effect
  
  /**
   * Deletes a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to delete
   * @return Unit on successful deletion
   */
  def delete(favorite: Article.FavoriteBy): Unit < Effect
}
