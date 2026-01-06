package conduit.domain.request.article

import ListArticlesRequest.Filter
import conduit.domain.model.User

case class ListArticlesRequest(
  requester: User,
  offset: Int,
  limit: Int,
  filters: List[Filter],
)

object ListArticlesRequest:
  enum Filter:
    case Tag(name: String)
    case Author(username: String)
    case FavoriteOf(username: String)
