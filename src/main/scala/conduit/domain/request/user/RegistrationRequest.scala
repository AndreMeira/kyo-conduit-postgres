package conduit.domain.request.user

import conduit.domain.model.User

case class RegistrationRequest(
  requester: User.Anonymous.type,
  payload: RegistrationRequest.Payload,
)

object RegistrationRequest:
  case class Payload(user: Data) // wrapping due to api spec
  case class Data(
    username: String,
    email: String,
    password: String,
  )                              // wrapping due to api spec
