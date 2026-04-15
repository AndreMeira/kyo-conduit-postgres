package conduit.domain.types

type ArticleDescription = ArticleDescription.Type
object ArticleDescription {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
