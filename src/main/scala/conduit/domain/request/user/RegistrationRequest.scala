package conduit.domain.request.user

import conduit.domain.model.User

/**
 * Represents a user registration request in the Conduit application.
 *
 * This request is used when new users sign up for an account. It requires
 * the requester to be anonymous (not already authenticated) and contains
 * the necessary user data for creating a new account.
 *
 * @param requester must be an anonymous user for registration
 * @param payload the registration data wrapped according to API specification
 */
case class RegistrationRequest(
  requester: User.Anonymous.type,
  payload: RegistrationRequest.Payload,
)

object RegistrationRequest:
  /**
   * Wrapper for registration data to match API specification format.
   *
   * @param user the actual user data for registration
   */
  case class Payload(user: Data) // wrapping due to api spec

  /**
   * Contains the actual user registration data fields.
   *
   * @param username the desired username for the new account
   * @param email the user's email address
   * @param password the user's password in plaintext (will be hashed)
   */
  case class Data(
    username: String,
    email: String,
    password: String,
  )                              // wrapping due to api spec
