package conduit.domain.types

type ProfileBiography = ProfileBiography.Type
object ProfileBiography {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
