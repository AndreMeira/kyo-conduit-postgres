package conduit.domain.request.article

import conduit.domain.model.User

case class AddFavoriteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
