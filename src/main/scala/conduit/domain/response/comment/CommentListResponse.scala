package conduit.domain.response.comment

import conduit.domain.model.{ Article, Comment, User, UserProfile }

import java.util.UUID
import scala.util.chaining.scalaUtilChainingOps

/**
 * Response model for retrieving a list of comments.
 *
 * @param comments List of comment payloads with author information
 */
case class CommentListResponse(comments: List[GetCommentResponse.Payload])

object CommentListResponse:
  /**
   * Creates a CommentListResponse from domain objects.
   *
   * @param comments List of comment domain objects
   * @param profiles List of user profiles for comment authors
   * @param followed Set of user IDs that the current user follows
   * @return CommentListResponse with transformed payloads
   */
  def make(
    comments: List[Comment],
    profiles: List[UserProfile],
    followed: Set[UUID],
  ): CommentListResponse =
    CommentListResponse(payload(comments, profiles, followed))

  /**
   * Transforms domain objects into comment payloads for the response.
   *
   * @param comments List of comment domain objects
   * @param profiles List of user profiles for comment authors
   * @param followed Set of user IDs that the current user follows
   * @return List of comment payloads with author information and following status
   */
  def payload(
    comments: List[Comment],
    profiles: List[UserProfile],
    followed: Set[UUID],
  ): List[GetCommentResponse.Payload] = {
    val profileByUserId = profiles.map(profile => profile.userId -> profile).toMap
    for {
      comment  <- comments
      profile  <- profileByUserId.get(comment.authorId)
      following = followed.contains(profile.id)
    } yield GetCommentResponse.payload(comment, profile, following)
  }
