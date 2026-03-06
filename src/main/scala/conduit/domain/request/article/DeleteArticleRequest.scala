package conduit.domain.request.article

import conduit.domain.model.User

/**
 * Represents a request to delete an article in the Conduit application.
 *
 * This request is used when an authenticated user wants to delete an article
 * they have authored. The requester must be authenticated and provide the
 * slug of the article to be deleted.
 *
 * @param requester the authenticated user making the request
 * @param slug the URL-friendly identifier of the article to be deleted
 */
case class DeleteArticleRequest(
  requester: User.Authenticated,
  slug: String,
)
