package conduit.domain.model

import conduit.domain.error.ValidationError
import conduit.domain.service.validation.CommonValidation
import conduit.domain.syntax.*

import java.time.Instant
import java.util.UUID

/**
 * Represents a comment on an article in the Conduit blogging platform.
 *
 * Comments allow users to engage in discussions about articles, providing feedback
 * and fostering community interaction. Each comment is associated with a specific
 * article and authored by a registered user.
 *
 * @param id the unique identifier for the comment
 * @param articleId the unique identifier of the article this comment belongs to
 * @param body the text content of the comment
 * @param authorId the unique identifier of the user who authored the comment
 * @param createdAt the timestamp when the comment was created
 * @param updatedAt the timestamp when the comment was last modified
 */
case class Comment(
  id: Comment.Id,
  articleId: Article.Id,
  body: String,
  authorId: UUID,
  createdAt: Instant,
  updatedAt: Instant,
)

object Comment:
  /** Type alias for comment identifiers using Long */
  type Id = Long

  case class Data(
    articleId: Article.Id,
    body: String,
    authorId: UUID,
    createdAt: Instant,
    updatedAt: Instant,
  ) {
    def withId(id: Long): Comment =
      Comment(
        id = id,
        articleId = articleId,
        body = body,
        authorId = authorId,
        createdAt = createdAt,
        updatedAt = updatedAt
      )

  }
