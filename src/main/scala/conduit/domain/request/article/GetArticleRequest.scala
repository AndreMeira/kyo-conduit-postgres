package conduit.domain.request.article

import conduit.domain.model.User

case class GetArticleRequest(
  requester: User,
  slug: String,
)
