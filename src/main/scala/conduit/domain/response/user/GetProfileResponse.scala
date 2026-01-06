package conduit.domain.response.user

import conduit.domain.model.UserProfile

case class GetProfileResponse(profile: GetProfileResponse.Payload)

object GetProfileResponse:
  case class Payload(
    username: String,
    bio: Option[String],
    image: Option[String],
    following: Boolean,
  )

  def make(user: UserProfile, following: Boolean): GetProfileResponse =
    GetProfileResponse(payload(user, following))

  def payload(user: UserProfile, following: Boolean): Payload =
    Payload(
      username = user.name,
      bio = user.biography.toOption,
      image = user.image.map(_.toString).toOption,
      following = following,
    )
