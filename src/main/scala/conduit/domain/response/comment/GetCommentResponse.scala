package conduit.domain.response.comment

import conduit.domain.model.{Comment, UserProfile}
import conduit.domain.response.user.GetProfileResponse

/**
 * Response model for retrieving a single comment.
 *
 * @param comment The comment payload with author information
 */
case class GetCommentResponse(comment: GetCommentResponse.Payload)

object GetCommentResponse:
  /**
   * Comment payload structure for JSON serialization.
   *
   * @param id Unique identifier of the comment
   * @param createdAt Timestamp when the comment was created
   * @param updatedAt Timestamp when the comment was last updated
   * @param body The comment text content
   * @param author Profile information of the comment author
   */
  case class Payload(
    id: Long,
    createdAt: String,
    updatedAt: String,
    body: String,
    author: GetProfileResponse.Payload,
  )

  /**
   * Creates a GetCommentResponse from domain objects.
   *
   * @param comment The comment domain object
   * @param author The author's profile information
   * @param following Whether the current user follows the author
   * @return GetCommentResponse with the transformed comment payload
   */
  def make(comment: Comment, author: UserProfile, following: Boolean): GetCommentResponse =
    GetCommentResponse(payload(comment, author, following))

  /**
   * Transforms a comment domain object into a response payload.
   *
   * @param comment The comment domain object
   * @param author The author's profile information
   * @param following Whether the current user follows the author
   * @return Comment payload with author information and following status
   */
  def payload(comment: Comment, author: UserProfile, following: Boolean): Payload =
    Payload(
      id = comment.id,
      body = comment.body,
      createdAt = comment.createdAt.toString,
      updatedAt = comment.updatedAt.toString,
      author = GetProfileResponse.make(author, following).profile,
    )
