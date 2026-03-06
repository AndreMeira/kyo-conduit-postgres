package conduit.domain.request.comment

import conduit.domain.model.User

/**
 * Represents a request to delete a comment from an article in the Conduit application.
 *
 * This request is used when an authenticated user wants to delete a specific comment
 * from an article. The requester must be authenticated and provide the slug of the article
 * along with the ID of the comment to be deleted.
 *
 * @param requester the authenticated user making the request
 * @param slug the URL-friendly identifier of the article from which the comment is being deleted
 * @param commentId the unique identifier of the comment to be deleted
 */
case class DeleteCommentRequest(
  requester: User.Authenticated,
  slug: String,
  commentId: Long,
)
