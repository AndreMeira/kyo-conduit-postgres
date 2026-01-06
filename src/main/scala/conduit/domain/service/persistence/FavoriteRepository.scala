package conduit.domain.service.persistence

import conduit.domain.model.*
import conduit.domain.error.ApplicationError
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
trait FavoriteRepository[Tx] {
  type Effect = Async & Abort[ApplicationError] & Env[Tx]
  
  /**
   * Checks if a favorite entry for an article by a user exists.
   *
   * @param favorite the favorite entry to check
   * @return true if the favorite entry exists, false otherwise
   */
  def exists(favorite: Article.FavoriteBy): Boolean < Effect

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
