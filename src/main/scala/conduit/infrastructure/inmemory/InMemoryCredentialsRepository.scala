package conduit.infrastructure.inmemory

import conduit.domain.model.*
import conduit.domain.service.persistence.CredentialsRepository
import conduit.infrastructure.inmemory.InMemoryState.Changed.{ Deleted, Inserted, Updated }
import conduit.infrastructure.inmemory.InMemoryState.RowReference.CredentialsRow
import kyo.*

/**
 * In-memory implementation of the CredentialRepository for the Conduit application.
 *
 * This repository stores user credentials in memory and provides operations for finding, saving,
 * updating, and deleting credentials. All operations are wrapped in InMemoryTransaction
 * to ensure consistent access to the shared credentials state.
 */
class InMemoryCredentialsRepository extends CredentialsRepository[InMemoryTransaction] {

  /**
   * Finds user ID by the given hashed credentials.
   *
   * @param creds the hashed credentials to search for
   * @return a Maybe containing the user ID if found, or None otherwise
   */
  override def find(creds: Credentials.Hashed): Maybe[User.Id] < Effect =
    InMemoryTransaction { state =>
      state.credentials.get.map(_.collectFirst { case (userId, `creds`) => userId }).map(Maybe.fromOption)
    }

  /**
   * Finds hashed credentials by user ID.
   *
   * @param userId the user ID to search for
   * @return a Maybe containing the hashed credentials if found, or None otherwise
   */
  override def find(userId: User.Id): Maybe[Credentials.Hashed] < Effect =
    InMemoryTransaction { state =>
      state.credentials.get.map(_.get(userId)).map(Maybe.fromOption)
    }

  /**
   * Checks if credentials exist for the given user ID.
   *
   * @param email the email to check
   * @return true if credentials exist, false otherwise
   */
  override def exists(email: Credentials.Email): Boolean < Effect =
    InMemoryTransaction { state =>
      state.credentials.get.map(_.exists((_, creds) => creds.email == email))
    }

  /**
   * Saves new credentials for a user.
   *
   * @param userId the user ID associated with the credentials
   * @param credentials the hashed credentials to save
   * @return Unit
   */
  override def save(userId: User.Id, credentials: Credentials.Hashed): Unit < Effect =
    InMemoryTransaction { state =>
      state.credentials.updateAndGet(_ + (userId -> credentials))
        *> state.addChange(Inserted(CredentialsRow(userId)))
    }

  /**
   * Updates existing credentials for a user.
   *
   * @param userId the user ID associated with the credentials
   * @param credentials the hashed credentials to update
   * @return Unit
   */
  override def update(userId: User.Id, credentials: Credentials.Hashed): Unit < Effect =
    InMemoryTransaction { state =>
      state.credentials.updateAndGet(_ + (userId -> credentials)).unit
        *> state.addChange(Updated(CredentialsRow(userId)))
    }

  /**
   * Deletes credentials for a user.
   *
   * @param userId the user ID whose credentials should be deleted
   * @return Unit
   */
  override def delete(userId: User.Id): Unit < Effect =
    InMemoryTransaction { state =>
      state.credentials.updateAndGet(_ - userId).unit
        *> state.addChange(Deleted(CredentialsRow(userId)))
    }
}
