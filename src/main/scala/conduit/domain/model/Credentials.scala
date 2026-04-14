package conduit.domain.model

import java.util.UUID

/**
 * Represents user authentication credentials in different states.
 *
 * Credentials can be in cleartext form (as received from user input) or in
 * hashed form (as stored securely in the database). This enum ensures type-safe
 * handling of sensitive authentication data throughout the application.
 */
enum Credentials:

  /** The user's email address associated with the credentials. */
  def email: String

  /**
   * Represents cleartext credentials as received from user input.
   *
   * This form is used temporarily during authentication and registration processes
   * before passwords are hashed for secure storage.
   *
   * @param email the user's email address
   * @param password the user's password in plaintext
   */
  case Clear(email: String, password: String)

  /**
   * Represents hashed credentials as stored in the database.
   *
   * This form contains the user ID, email, and securely hashed password.
   * It's used for authentication verification and secure storage.
   *
   * @param email the user's email address
   * @param password the user's password after secure hashing
   */
  case Hashed(email: String, password: String)

object Credentials:
  /** Type alias for email addresses and password */
  type Email    = String
  type Password = String
