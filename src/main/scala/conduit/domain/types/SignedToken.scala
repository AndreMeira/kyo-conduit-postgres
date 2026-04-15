package conduit.domain.types

type SignedToken = SignedToken.Type
object SignedToken {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
