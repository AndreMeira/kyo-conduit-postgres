package conduit.domain.request.article

import conduit.domain.model.User

case class DeleteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
