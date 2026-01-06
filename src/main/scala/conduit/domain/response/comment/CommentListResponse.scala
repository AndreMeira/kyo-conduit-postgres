package conduit.domain.response.comment

import conduit.domain.model.{Article, Comment, User, UserProfile}

import java.util.UUID
import scala.util.chaining.scalaUtilChainingOps

case class CommentListResponse(comments: List[GetCommentResponse.Payload])

object CommentListResponse:
  def make(
    comments: List[Comment],
    profiles: List[UserProfile],
    followed: Set[UUID],
  ): CommentListResponse =
    CommentListResponse(payload(comments, profiles, followed))

  def payload(
    comments: List[Comment],
    profiles: List[UserProfile],
    followed: Set[UUID],
  ): List[GetCommentResponse.Payload] = {
    val profileById = profiles.map(profile => profile.id -> profile).toMap
    for {
      comment  <- comments
      profile  <- profileById.get(comment.authorId)
      following = followed.contains(profile.id)
    } yield GetCommentResponse.payload(comment, profile, following)
  }
