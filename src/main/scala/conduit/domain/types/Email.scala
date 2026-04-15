package conduit.domain.types

type Email = Email.Type
object Email {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
