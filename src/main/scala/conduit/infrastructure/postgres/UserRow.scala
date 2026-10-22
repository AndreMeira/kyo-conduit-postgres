package conduit.infrastructure.postgres

import conduit.domain.model.{ Credentials, User }

/**
 * The credentials columns of a row of the `users` table.
 *
 * @param id the user
 * @param email the user's email
 * @param password the user's hashed password
 */
case class UserRow(id: User.Id, email: Credentials.Email, password: Credentials.Password)
