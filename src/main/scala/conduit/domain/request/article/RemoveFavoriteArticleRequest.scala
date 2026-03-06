package conduit.domain.request.article

import conduit.domain.model.User

/**
 * Represents a request to remove an article from the authenticated user's favorites.
 *
 * This request is used when an authenticated user wants to unfavorite an article.
 * The requester must be authenticated and provide the slug of the article to be removed
 * from their favorites.
 *
 * @param requester the authenticated user making the request
 * @param slug the URL-friendly identifier of the article to be unfavorited
 */
case class RemoveFavoriteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
