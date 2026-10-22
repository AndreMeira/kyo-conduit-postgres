package conduit.infrastructure.postgres

import conduit.domain.model.{ Credentials, User }
import conduit.domain.service.persistence.CredentialsRepository
import conduit.infrastructure.codecs.database.SqlSchemaCodecs.given
import PostgresTransaction.Transactional
import kyo.*

/**
 * Postgres implementation of [[CredentialsRepository]], over the `users` table.
 */
class PostgresCredentialsRepository extends CredentialsRepository[PostgresTransaction]:

  /**
   * Finds the user holding exactly these credentials.
   *
   * @param creds the email and hashed password
   * @return the user, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def find(creds: Credentials.Hashed): Maybe[User.Id] < Effect =
    Transactional:
      Sql.from[UserRow]("u", "users")
        .where(row => row.u.email == creds.email && row.u.password == creds.password)
        .select(_.u.id)
        .run
        .map(ids => Maybe.fromOption(ids.headOption))

  /**
   * Finds a user's credentials.
   *
   * @param userId the user
   * @return the credentials, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def find(userId: User.Id): Maybe[Credentials.Hashed] < Effect =
    Transactional:
      Sql.from[UserRow]("u", "users")
        .where(_.u.id == userId)
        .run
        .map(rows => Maybe.fromOption(rows.headOption).map(row => new Credentials.Hashed(row.email, row.password)))

  /**
   * Whether a user holds this email.
   *
   * @param email the email
   * @return true when it is taken; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def exists(email: Credentials.Email): Boolean < Effect =
    Transactional:
      Sql.from[UserRow]("u", "users").where(_.u.email == email).limit(1).run.map(_.nonEmpty)

  /**
   * Saves a new user with its credentials.
   *
   * @param userId the user
   * @param credentials the email and hashed password
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the insert fails
   */
  override def save(userId: User.Id, credentials: Credentials.Hashed): Unit < Effect =
    Transactional.single("users"):
      Sql.insert[UserRow]("users")
        .values(UserRow(userId, credentials.email, credentials.password))
        .run
        .map(_.affectedRows)

  /**
   * Replaces a user's credentials.
   *
   * @param userId the user
   * @param credentials the new email and hashed password
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the update fails, or with
   *         `PostgresDatabase.Error.UnexpectedRowCount` when no user has this id
   */
  override def update(userId: User.Id, credentials: Credentials.Hashed): Unit < Effect =
    Transactional.single("users"):
      Sql.update[UserRow]("users")
        .set(_.email := credentials.email, _.password := credentials.password)
        .where(_.id == userId)
        .run

  /**
   * Deletes a user; one that does not exist is ignored.
   *
   * @param userId the user
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the delete fails
   */
  override def delete(userId: User.Id): Unit < Effect =
    Transactional:
      Sql.delete[UserRow]("users").where(_.id == userId).run.unit
