package conduit.domain.types

type FavoriteCount = FavoriteCount.Type
object FavoriteCount {
  opaque type Type <: Int = Int
  def apply(value: Int): Type = value
}
