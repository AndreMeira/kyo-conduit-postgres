package conduit.domain.types

type TagName = TagName.Type
object TagName {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
