package conduit.domain.response.user

import conduit.domain.model.{User, UserProfile}

case class AuthenticationResponse(user: AuthenticationResponse.Payload)

object AuthenticationResponse:
  case class Payload(
    email: String,
    token: String,
    username: String,
    bio: Option[String],
    image: Option[String],
  )

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
