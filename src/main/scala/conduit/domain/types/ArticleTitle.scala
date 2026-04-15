package conduit.domain.types

type ArticleTitle = ArticleTitle.Type
object ArticleTitle {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
