package conduit.domain.request.article

import conduit.domain.model.User

/**
 * Represents a request to add an article to a user's list of favorite articles.
 *
 * This request encapsulates the necessary information to identify both the requester
 * (the authenticated user) and the article to be favorited (identified by its slug).
 *
 * @param requester the authenticated user making the request
 * @param slug the URL-friendly identifier of the article to be favorited
 */
case class AddFavoriteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
