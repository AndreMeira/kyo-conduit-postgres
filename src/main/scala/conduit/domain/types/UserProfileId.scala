package conduit.domain.types

import java.util.UUID

type UserProfileId = UserProfileId.Type
object UserProfileId {
  opaque type Type <: UUID = UUID
  def apply(value: UUID): Type = value
}
