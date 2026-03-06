package conduit.domain.request.user

import conduit.domain.error.ValidationError

import scala.util.matching.compat.Regex

/**
 * Represents validation errors related to user profile input.
 *
 * This enum covers cases such as username already existing, invalid characters in the username,
 * and username length violations. Each case provides a descriptive error message.
 */
enum InvalidInput extends ValidationError.InvalidInput {

  /**
   * Indicates that the provided username is already taken by another user.
   * @param username The username that already exists.
   */
  case UsernameAlreadyExists(username: String)

  /**
   * Indicates that the username contains invalid characters.
   * @param allowed The regular expression describing allowed characters.
   */
  case UserNameInvalidChar(allowed: Regex)

  /**
   * Indicates that the username does not meet length requirements.
   * @param value The username value.
   * @param min The minimum allowed length.
   * @param max The maximum allowed length.
   */
  case UserNameLengthViolation(value: String, min: Int, max: Int)

  /**
   * Indicates that the provided image URI is invalid.
   * @param value The invalid image URI string.
   */
  case InvalidImageUri(value: String)

  /**
   * Indicates that the biography field is empty.
   */
  case BiographyIsEmpty

  /**
   * Returns a human-readable error message for each validation error.
   */
  override def message: String = this match {
    case UsernameAlreadyExists(username)          => s"Username $username already exists"
    case UserNameInvalidChar(_)                   => s"Use name must contain only alphanumeric characters"
    case UserNameLengthViolation(value, min, max) => s"User name must be between $min and $max characters"
    case BiographyIsEmpty                         => s"Biography cannot be empty"
    case InvalidImageUri(value)                   => s"Image URI '$value' is not valid"
  }
}
