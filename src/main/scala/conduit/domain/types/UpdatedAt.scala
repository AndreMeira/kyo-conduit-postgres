package conduit.domain.types

import java.time.Instant

type UpdatedAt = UpdatedAt.Type
object UpdatedAt {
  opaque type Type <: Instant = Instant
  def apply(value: Instant): Type = value
}
