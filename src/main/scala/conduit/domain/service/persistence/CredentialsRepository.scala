package conduit.domain.service.persistence

import conduit.domain.error.ApplicationError
import conduit.domain.model
import conduit.domain.model.{ Credentials, User }
import conduit.domain.service.persistence.Database.Transaction
import kyo.*

/**
 * Repository trait for managing credential persistence operations.
 *
 * This trait defines the contract for credentials storage and retrieval,
 * allowing different implementations (in-memory, database, etc.) to provide
 * persistent storage for user credentials in the Conduit application.
 *
 * Credentials are stored in a hashed format for security purposes and are
 * associated with user accounts. This repository handles CRUD operations
 * for managing user authentication credentials.
 *
 * @tparam Tx the transaction type used for database operations
 */
trait CredentialsRepository[Tx <: Transaction] {
  type Effect = Async & Abort[ApplicationError] & Env[Tx]

  /**
   * Finds user ID by hashed credentials.
   *
   * Retrieves the user ID associated with the provided hashed credentials.
   * Returns None if no matching credentials are found.
   *
   * @param creds the hashed credentials to search for
   * @return a Maybe containing the user ID if found, or None if not found
   */
  def find(creds: Credentials.Hashed): Maybe[User.Id] < Effect

  /**
   * Finds hashed credentials by user ID.
   *
   * Retrieves the hashed credentials associated with the specified user ID.
   * Returns None if no credentials are found for the user.
   *
   * @param userId the user ID to search for
   * @return a Maybe containing the hashed credentials if found, or None otherwise
   */
  def find(userId: User.Id): Maybe[Credentials.Hashed] < Effect

  /**
   * Checks if credentials exist for the given user ID.
   *
   * Provides a quick way to verify whether a user has credentials stored
   * without retrieving the full credential data.
   *
   * @param email the email to check
   * @return true if credentials exist, false otherwise
   */
  def exists(email: Credentials.Email): Boolean < Effect

  /**
   * Saves new credentials for a user.
   *
   * Stores hashed credentials for a user account. This operation is typically
   * performed during user registration or initial credential setup.
   *
   * @param userId the user ID associated with the credentials
   * @param credentials the hashed credentials to save
   * @return Unit on successful save
   */
  def save(userId: User.Id, credentials: Credentials.Hashed): Unit < Effect

  /**
   * Updates existing credentials for a user.
   *
   * Replaces the current credentials for a user with new hashed credentials.
   * This operation is typically performed during password changes or credential
   * updates.
   *
   * @param userId the user ID associated with the credentials
   * @param credentials the hashed credentials to update
   * @return Unit on successful update
   */
  def update(userId: User.Id, credentials: Credentials.Hashed): Unit < Effect

  /**
   * Deletes credentials for a user.
   *
   * Removes credentials from the repository, typically when deactivating or
   * deleting a user account.
   *
   * @param userId the user ID whose credentials should be deleted
   * @return Unit on successful deletion
   */
  def delete(userId: User.Id): Unit < Effect
}
