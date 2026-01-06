package conduit.domain.request.user

import conduit.domain.model.User

case class GetProfileRequest(
  requester: User,
  username: String,
)
