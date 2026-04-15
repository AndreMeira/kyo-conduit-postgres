package conduit.domain.types

type CommentBody = CommentBody.Type
object CommentBody {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
