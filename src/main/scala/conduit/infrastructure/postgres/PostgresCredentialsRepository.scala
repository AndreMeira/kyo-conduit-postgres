package conduit.infrastructure.postgres

import com.augustnagro.magnum.*
import conduit.domain.model.{ Credentials, User }
import conduit.domain.service.persistence.CredentialsRepository
import conduit.infrastructure.postgres.PostgresTransaction.Transactional
import kyo.*

class PostgresCredentialsRepository extends CredentialsRepository[PostgresTransaction]:

  /**
   * Finds a user ID by hashed credentials (email + password).
   *
   * @param creds the hashed credentials to search for
   * @return a Maybe containing the user ID if found, or None if not found
   */
  override def find(creds: Credentials.Hashed): Maybe[User.Id] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT id FROM users
              WHERE email = ${creds.email} AND password = ${creds.password}
            """.query[User.Id].run().headOption

  /**
   * Finds hashed credentials by user ID.
   *
   * @param userId the user ID to search for
   * @return a Maybe containing the hashed credentials if found, or None otherwise
   */
  override def find(userId: User.Id): Maybe[Credentials.Hashed] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT email, password FROM users WHERE id = $userId"""
          .query[(String, String)]
          .run()
          .headOption
          .map(Credentials.Hashed.apply)

  /**
   * Checks if credentials exist for the given email address.
   *
   * @param email the email to check
   * @return true if credentials exist, false otherwise
   */
  override def exists(email: Credentials.Email): Boolean < Effect =
    Transactional:
      sql"""SELECT EXISTS(SELECT 1 FROM users WHERE email = $email)"""
        .query[Boolean]
        .run()
        .headOption
        .contains(true)

  /**
   * Saves new credentials for a user, inserting a row into the users table.
   *
   * @param userId the user ID associated with the credentials
   * @param credentials the hashed credentials to save
   * @return Unit on successful save
   */
  override def save(userId: User.Id, credentials: Credentials.Hashed): Unit < Effect =
    Transactional:
      val count = sql"""
          INSERT INTO users (id, email, password)
          VALUES ($userId, ${credentials.email}, ${credentials.password})
        """.update.run()
      require(count == 1, "Failed to insert credentials")

  /**
   * Updates existing credentials for a user.
   *
   * @param userId the user ID associated with the credentials
   * @param credentials the hashed credentials to update
   * @return Unit on successful update
   */
  override def update(userId: User.Id, credentials: Credentials.Hashed): Unit < Effect =
    Transactional:
      val count = sql"""
          UPDATE users SET
            email    = ${credentials.email},
            password = ${credentials.password}
          WHERE id = $userId
        """.update.run()
      require(count == 1, "Failed to update credentials")

  /**
   * Deletes credentials for a user by removing their row from the users table.
   *
   * @param userId the user ID whose credentials should be deleted
   * @return Unit on successful deletion
   */
  override def delete(userId: User.Id): Unit < Effect =
    Transactional {
      sql"""DELETE FROM users WHERE id = $userId""".update
        .run()
    }.unit
