package conduit.domain.types

type Password = Password.Type
object Password {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
