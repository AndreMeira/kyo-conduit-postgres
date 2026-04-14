package conduit.domain.error

import conduit.domain.error.ValidationError
import conduit.domain.model.Credentials

/**
 * Represents validation errors related to user credentials input.
 * 
 * This enum covers cases such as invalid email format, email already in use,
 * and invalid password format. Each case provides a descriptive error message.
 */
enum CredentialsInvalidInput extends ValidationError.InvalidInput {

  /**
   * Indicates that the email field is empty.
   */
  case EmptyEmail

  /**
   * Indicates that the password field is empty.
   */
  case EmptyPassword

  /**
   * Indicates that the provided email does not match the expected format.
   * @param email The invalid email string.
   */
  case InvalidEmailFormat(email: String)

  /**
   * Indicates that the provided email is already registered in the system.
   * @param email The email address that is already in use.
   */
  case EmailAlreadyInUse(email: Credentials.Email)

  /**
   * Indicates that the provided password does not match the expected format.
   * @param pwd The invalid password string.
   * @param expected A description of the expected password format.
   */
  case InvalidPasswordFormat(pwd: String, expected: String)

  /**
   * Returns a human-readable error message for each validation error.
   */
  override def message: String = this match {
    case EmptyEmail                           => "Email cannot be empty."
    case EmptyPassword                        => "Password cannot be empty."
    case EmailAlreadyInUse(email)             => s"The email address '$email' is already in use."
    case InvalidEmailFormat(email)            => s"The email address '$email' is not in a valid format."
    case InvalidPasswordFormat(pwd, expected) => s"The provided password '$pwd' is invalid. Expected format: $expected"
  }
}
