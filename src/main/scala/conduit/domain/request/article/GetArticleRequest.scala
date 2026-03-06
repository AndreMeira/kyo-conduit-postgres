package conduit.domain.request.article

import conduit.domain.model.User

/**
 * Represents a request to retrieve a specific article by its slug.
 *
 * This request includes the requester (which can be an authenticated or unauthenticated user)
 * and the slug of the article to be retrieved.
 *
 * @param requester the user making the request, can be authenticated or unauthenticated
 * @param slug the URL-friendly identifier of the article to be retrieved
 */
case class GetArticleRequest(
  requester: User,
  slug: String,
)
