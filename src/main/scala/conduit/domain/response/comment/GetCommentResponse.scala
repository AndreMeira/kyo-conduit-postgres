package conduit.domain.response.comment

import conduit.domain.model.{Comment, UserProfile}
import conduit.domain.response.user.GetProfileResponse

case class GetCommentResponse(comment: GetCommentResponse.Payload)

object GetCommentResponse:
  case class Payload(
    id: Long,
    createdAt: String,
    updatedAt: String,
    body: String,
    author: GetProfileResponse.Payload,
  )

  def make(comment: Comment, author: UserProfile, following: Boolean): GetCommentResponse =
    GetCommentResponse(payload(comment, author, following))

  def payload(comment: Comment, author: UserProfile, following: Boolean): Payload =
    Payload(
      id = comment.id,
      body = comment.body,
      createdAt = comment.createdAt.toString,
      updatedAt = comment.updatedAt.toString,
      author = GetProfileResponse.make(author, following).profile,
    )
