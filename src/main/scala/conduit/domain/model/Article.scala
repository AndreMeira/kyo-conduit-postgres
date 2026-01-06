package conduit.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Represents an article in the Conduit blogging platform.
 *
 * Articles are the core content entities that users can create, read, update, and favorite.
 * Each article has metadata including authorship, timestamps, and associated tags.
 *
 * @param id the unique identifier for the article
 * @param slug the URL-friendly identifier used in routes
 * @param title the article title
 * @param description a brief summary or description of the article
 * @param body the main content of the article
 * @param authorId the unique identifier of the user who authored the article
 * @param favoriteCount the number of users who have favorited this article
 * @param tags a list of tags associated with the article for categorization
 * @param createdAt the timestamp when the article was created
 * @param updatedAt the timestamp when the article was last modified
 */
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
  /** Type alias for article identifiers using UUID */
  type Id = UUID

  /**
   * Represents a user's favorite relationship with an article.
   *
   * This is used to track which users have favorited which articles,
   * enabling features like favorite counts and personal favorite lists.
   *
   * @param userId the unique identifier of the user who favorited the article
   * @param articleId the unique identifier of the favorited article
   */
  case class FavoriteBy(
    userId: User.Id,
    articleId: Article.Id,
  )
