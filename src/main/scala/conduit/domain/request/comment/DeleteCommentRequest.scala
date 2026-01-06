package conduit.domain.request.comment

import conduit.domain.model.User

case class DeleteCommentRequest(
  requester: User.Authenticated,
  slug: String,
  commentId: Long,
)
