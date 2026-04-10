package conduit.domain.response.user

import conduit.domain.model.{ User, UserProfile }

/**
 * Represents the response returned after successful user authentication.
 *
 * This response contains the authenticated user's information along with
 * a signed authentication token that can be used for subsequent API requests.
 *
 * @param user the authenticated user data wrapped according to API specification
 */
case class AuthenticationResponse(user: AuthenticationResponse.Payload)

object AuthenticationResponse:
  /**
   * Contains the authenticated user's data and authentication token.
   *
   * @param email the user's email address
   * @param token the signed authentication token for API access
   * @param username the user's display name
   * @param bio optional biographical information
   * @param image optional profile image URL
   */
  case class Payload(
    email: String,
    token: String,
    username: String,
    bio: Option[String],
    image: Option[String],
  )

  /**
   * Creates an AuthenticationResponse from domain objects.
   *
   * This factory method constructs the response by combining user credentials,
   * profile information, and authentication token into the API-compliant format.
   *
   * @param email the user's email address from credentials
   * @param profile the user's profile containing display information
   * @param token the signed authentication token
   * @return an AuthenticationResponse ready for API consumption
   */
  def make(
    email: String,
    profile: UserProfile,
    token: User.SignedToken,
  ): AuthenticationResponse = AuthenticationResponse(
    Payload(
      email = email,
      token = token.value,
      username = profile.name,
      bio = profile.biography.toOption,
      image = profile.image.map(_.toString).toOption,
    )
  )
