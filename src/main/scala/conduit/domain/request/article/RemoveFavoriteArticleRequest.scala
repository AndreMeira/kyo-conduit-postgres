package conduit.domain.request.article

import conduit.domain.model.User

case class RemoveFavoriteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
