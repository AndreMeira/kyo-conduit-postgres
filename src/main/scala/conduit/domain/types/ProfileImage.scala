package conduit.domain.types

import java.net.URI

type ProfileImage = ProfileImage.Type
object ProfileImage {
  opaque type Type <: URI = URI
  def apply(value: URI): Type = value
}
