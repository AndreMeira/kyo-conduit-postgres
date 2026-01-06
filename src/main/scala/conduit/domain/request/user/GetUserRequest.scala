package conduit.domain.request.user

import conduit.domain.model.User

case class GetUserRequest(requester: User.Authenticated)
