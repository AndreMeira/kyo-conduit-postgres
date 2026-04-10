package conduit.domain.request.article

import ListArticlesRequest.Filter
import conduit.domain.model.User

/**
 * Represents a request to list articles with optional filtering criteria.
 *
 * This request includes the requester (which can be an authenticated or unauthenticated user),
 * pagination parameters, and a list of filters to apply when retrieving articles.
 *
 * @param requester the user making the request, can be authenticated or unauthenticated
 * @param offset the number of articles to skip before starting to collect the result set
 * @param limit the maximum number of articles to return
 * @param filters a list of filters to apply when retrieving articles
 */
case class ListArticlesRequest(
  requester: User,
  offset: Int,
  limit: Int,
  filters: List[Filter],
)

object ListArticlesRequest:

  /**
   * Represents the different types of filters that can be applied when listing articles.
   */
  enum Filter:
    case Tag(name: String)
    case Author(username: String)
    case FavoriteOf(username: String)
