package conduit.domain.response.article

import conduit.domain.model.{ Article, UserProfile }
import conduit.domain.response.user.GetProfileResponse

/**
 * Response model for retrieving a single article.
 *
 * @param article The article payload with author information
 */
case class GetArticleResponse(article: GetArticleResponse.Payload)

object GetArticleResponse:
  /**
   * Article payload structure for JSON serialization.
   *
   * @param slug URL-friendly identifier for the article
   * @param title The article title
   * @param description Brief description of the article
   * @param body The full article content
   * @param tagList List of tags associated with the article
   * @param createdAt Timestamp when the article was created
   * @param updatedAt Timestamp when the article was last updated
   * @param favorited Whether the current user has favorited this article
   * @param favoritesCount Number of users who have favorited this article
   * @param author Profile information of the article author
   */
  case class Payload(
    slug: String,
    title: String,
    description: String,
    body: String,
    tagList: List[String],
    createdAt: String,
    updatedAt: String,
    favorited: Boolean,
    favoritesCount: Int,
    author: GetProfileResponse.Payload,
  )

  /**
   * Creates a GetArticleResponse from domain objects.
   *
   * @param article The article domain object
   * @param profile The author's profile information
   * @param favorited Whether the current user has favorited the article
   * @param following Whether the current user follows the author
   * @return GetArticleResponse with the transformed article payload
   */
  def make(
    article: Article,
    profile: UserProfile,
    favorited: Boolean,
    following: Boolean,
  ): GetArticleResponse = GetArticleResponse(
    Payload(
      slug = article.slug,
      title = article.title,
      description = article.description,
      body = article.body,
      tagList = article.tags,
      createdAt = article.createdAt.toString,
      updatedAt = article.updatedAt.toString,
      favorited = favorited,
      favoritesCount = article.favoriteCount,
      author = GetProfileResponse.payload(profile, following),
    )
  )
