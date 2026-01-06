package conduit.domain.request.article

import conduit.domain.model.User

case class ArticleFeedRequest(
  requester: User.Authenticated,
  offset: Int,
  limit: Int,
)
