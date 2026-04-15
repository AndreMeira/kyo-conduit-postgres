package conduit.domain.types

type ProfileName = ProfileName.Type
object ProfileName {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
