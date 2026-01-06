package conduit.domain.request.comment

import conduit.domain.model.User

case class ListCommentsRequest(requester: User, slug: String)
