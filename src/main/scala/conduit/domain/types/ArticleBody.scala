package conduit.domain.types

type ArticleBody = ArticleBody.Type
object ArticleBody {
  opaque type Type <: String = String
  def apply(value: String): Type = value
}
