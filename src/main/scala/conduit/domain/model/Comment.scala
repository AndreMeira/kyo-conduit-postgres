package conduit.domain.model

import conduit.domain.error.ValidationError
import conduit.domain.service.validation.CommonValidation
import conduit.domain.syntax.*

import java.time.Instant
import java.util.UUID

case class Comment(
  id: Long,
  articleId: Article.Id,
  body: String,
  authorId: UUID,
  createdAt: Instant,
  updatedAt: Instant,
)

object Comment:
  type Id = Long
