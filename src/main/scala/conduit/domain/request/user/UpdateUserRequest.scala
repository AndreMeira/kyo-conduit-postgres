package conduit.domain.request.user

import conduit.domain.model.{User, UserProfile}
import kyo.Maybe

case class UpdateUserRequest(
  requester: User.Authenticated,
  payload: UpdateUserRequest.Payload,
)

object UpdateUserRequest:
  case class Payload(user: Data) // wrapping due to api spec

  case class Data(
    email: Option[String],
    username: Option[String],
    password: Option[String],
    bio: Option[String],
    image: Option[String],
  )