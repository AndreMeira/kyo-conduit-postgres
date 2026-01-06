package conduit.domain.request.comment

import conduit.domain.model.User

case class AddCommentRequest(
  requester: User.Authenticated,
  slug: String,
  payload: AddCommentRequest.Payload,
)

object AddCommentRequest:
  case class Payload(comment: Data) // wrapping due to api spec
  case class Data(body: String)
