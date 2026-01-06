package conduit.domain.model

import java.time.Instant
import java.util.UUID

case class Article(
  id: Article.Id,
  slug: String,
  title: String,
  description: String,
  body: String,
  authorId: User.Id,
  favoriteCount: Int,
  tags: List[String],
  createdAt: Instant,
  updatedAt: Instant,
)

object Article:
  type Id = UUID

  case class FavoriteBy(
    userId: User.Id,
    articleId: Article.Id,
  )
