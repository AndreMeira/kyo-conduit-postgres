package conduit.domain.request.user

import conduit.domain.model.User

case class FollowUserRequest(
  requester: User.Authenticated,
  username: String,
)
