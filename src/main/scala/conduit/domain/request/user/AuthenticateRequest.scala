package conduit.domain.request.user

import conduit.domain.model.User

case class AuthenticateRequest(
  requester: User.Anonymous.type,
  payload: AuthenticateRequest.Payload,
)

object AuthenticateRequest:
  case class Payload(user: Data)                   // wrapping due to api spec
  case class Data(email: String, password: String) // wrapping due to api spec
