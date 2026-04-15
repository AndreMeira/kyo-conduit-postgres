package conduit.domain.types

import java.util.UUID

type UserId = UserId.Type
object UserId {
  opaque type Type <: UUID = UUID
  def apply(value: UUID): Type = value
}
