package conduit.domain.request.user

import conduit.domain.model.User

/**
 * Represents a user authentication (login) request in the Conduit application.
 *
 * This request is used when existing users attempt to log into their accounts.
 * The requester must be anonymous (not already authenticated) and provide
 * valid email and password credentials.
 *
 * @param requester must be an anonymous user for authentication
 * @param payload the authentication credentials wrapped according to API specification
 */
case class AuthenticateRequest(
  requester: User.Anonymous.type,
  payload: AuthenticateRequest.Payload,
)

object AuthenticateRequest:
  /**
   * Wrapper for authentication data to match API specification format.
   *
   * @param user the actual user credentials for authentication
   */
  case class Payload(user: Data)                   // wrapping due to api spec

  /**
   * Contains the user credentials for authentication.
   *
   * @param email the user's email address
   * @param password the user's password in plaintext for verification
   */
  case class Data(email: String, password: String) // wrapping due to api spec
