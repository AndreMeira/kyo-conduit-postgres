package conduit.domain.request.user

import conduit.domain.model.User

case class UnfollowUserRequest(
  requester: User.Authenticated,
  username: String,
)
