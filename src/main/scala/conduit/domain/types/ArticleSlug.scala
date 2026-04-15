package conduit.domain.types

type ArticleSlug = ArticleSlug.Type
object ArticleSlug {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
