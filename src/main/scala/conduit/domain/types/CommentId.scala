package conduit.domain.types

type CommentId = CommentId.Type
object CommentId {
  opaque type Type <: Long = Long
  def apply(value: Long): Type = value
}
