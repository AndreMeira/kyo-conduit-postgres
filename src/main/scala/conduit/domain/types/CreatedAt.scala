package conduit.domain.types

import java.time.Instant

type CreatedAt = CreatedAt.Type
object CreatedAt {
  opaque type Type <: Instant = Instant
  def apply(value: Instant): Type = value
}
