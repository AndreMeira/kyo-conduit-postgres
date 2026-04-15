package conduit.domain.model

import conduit.domain.types

/**
 * Represents user authentication credentials in different states.
 *
 * Credentials can be in cleartext form (as received from user input) or in
 * hashed form (as stored securely in the database). This enum ensures type-safe
 * handling of sensitive authentication data throughout the application.
 */
enum Credentials:

  /** The user's email address associated with the credentials. */
  def email: types.Email

  /**
   * Represents cleartext credentials as received from user input.
   *
   * This form is used temporarily during authentication and registration processes
   * before passwords are hashed for secure storage.
   *
   * @param email the user's email address
   * @param password the user's password in plaintext
   */
  case Clear(email: types.Email, password: types.Password)

  /**
   * Represents hashed credentials as stored in the database.
   *
   * This form contains the user ID, email, and securely hashed password.
   * It's used for authentication verification and secure storage.
   *
   * @param email the user's email address
   * @param password the user's password after secure hashing
   */
  case Hashed(email: types.Email, password: types.Password)

object Credentials:
  /** Type alias for email addresses and password */
  type Email    = types.Email
  type Password = types.Password
