package conduit.domain.request.comment

import conduit.domain.model.User

/**
 * Represents a request to add a comment to an article in the Conduit application.
 *
 * This request is used when an authenticated user wants to add a comment to a specific article.
 * The requester must be authenticated and provide the slug of the article along with the comment payload.
 *
 * @param requester the authenticated user making the request
 * @param slug the URL-friendly identifier of the article to which the comment is being added
 * @param payload the payload containing the comment data
 */
case class AddCommentRequest(
  requester: User.Authenticated,
  slug: String,
  payload: AddCommentRequest.Payload,
)

object AddCommentRequest:
  
  /**
   * Represents the payload for adding a comment.
   *
   * @param comment the comment data
   */
  case class Payload(comment: Data)
  
  /**
   * Represents the data for adding a comment.
   *
   * @param body the body content of the comment
   */
  case class Data(body: String)
