package conduit.domain.response.user

import conduit.domain.model.UserProfile

/**
 * Response model for retrieving a user profile.
 *
 * @param profile The user profile payload with following status
 */
case class GetProfileResponse(profile: GetProfileResponse.Payload)

object GetProfileResponse:
  /**
   * User profile payload structure for JSON serialization.
   *
   * @param username The user's display name
   * @param bio Optional biographical information about the user
   * @param image Optional URL to the user's profile image
   * @param following Whether the current user follows this profile
   */
  case class Payload(
    username: String,
    bio: Option[String],
    image: Option[String],
    following: Boolean,
  )

  /**
   * Creates a GetProfileResponse from a user profile domain object.
   *
   * @param user The user profile domain object
   * @param following Whether the current user follows this profile
   * @return GetProfileResponse with the transformed profile payload
   */
  def make(user: UserProfile, following: Boolean): GetProfileResponse =
    GetProfileResponse(payload(user, following))

  /**
   * Transforms a user profile domain object into a response payload.
   *
   * @param user The user profile domain object
   * @param following Whether the current user follows this profile
   * @return Profile payload with following status
   */
  def payload(user: UserProfile, following: Boolean): Payload =
    Payload(
      username = user.name,
      bio = user.biography.toOption,
      image = user.image.map(_.toString).toOption,
      following = following,
    )
