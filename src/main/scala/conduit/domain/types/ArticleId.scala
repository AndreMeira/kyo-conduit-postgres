package conduit.domain.types

import java.util.UUID

type ArticleId = ArticleId.Type
object ArticleId {
  opaque type Type <: UUID = UUID
  def apply(value: UUID): Type = value
}
