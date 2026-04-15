package conduit.domain.model

import conduit.domain.types.{ Email as UserEmail, Password as UserPassword }

/**
 * Represents user authentication credentials in different states.
 *
 * Credentials can be in cleartext form (as received from user input) or in
 * hashed form (as stored securely in the database). This enum ensures type-safe
 * handling of sensitive authentication data throughout the application.
 */
enum Credentials:

  /** The user's email address associated with the credentials. */
  def email: UserEmail

  /**
   * Represents cleartext credentials as received from user input.
   *
   * This form is used temporarily during authentication and registration processes
   * before passwords are hashed for secure storage.
   *
   * @param email the user's email address
   * @param password the user's password in plaintext
   */
  case Clear(email: UserEmail, password: UserPassword)

  /**
   * Represents hashed credentials as stored in the database.
   *
   * This form contains the user ID, email, and securely hashed password.
   * It's used for authentication verification and secure storage.
   *
   * @param email the user's email address
   * @param password the user's password after secure hashing
   */
  case Hashed(email: UserEmail, password: UserPassword)

object Credentials:
  /** Type alias for email addresses and password */
  type Email    = UserEmail
  type Password = UserPassword
