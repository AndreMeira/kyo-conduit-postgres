package conduit.domain.response.article

import conduit.domain.model.{ Article, UserProfile }
import conduit.domain.response.user.GetProfileResponse

import java.util.UUID

/**
 * Response model for retrieving a list of articles.
 *
 * @param articles List of article payloads with author information
 * @param articlesCount Total number of articles matching the query (for pagination)
 */
case class ArticleListResponse(
  articles: List[ArticleListResponse.Payload],
  articlesCount: Int,
)

object ArticleListResponse:
  /**
   * Article payload structure for JSON serialization in list responses.
   *
   * @param slug URL-friendly identifier for the article
   * @param title The article title
   * @param description Brief description of the article
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
    tagList: List[String],
    createdAt: String,
    updatedAt: String,
    favorited: Boolean,
    favoritesCount: Int,
    author: GetProfileResponse.Payload,
  )

  /**
   * Creates an ArticleListResponse from domain objects.
   *
   * @param count Total number of articles matching the query
   * @param articles List of article domain objects
   * @param profiles List of user profiles for article authors
   * @param favorites Set of article IDs that the current user has favorited
   * @param followed Set of user IDs that the current user follows
   * @return ArticleListResponse with transformed article payloads and metadata
   */
  def make(
    count: Int,
    articles: List[Article],
    profiles: List[UserProfile],
    favorites: Set[UUID],
    followed: Set[UUID],
  ): ArticleListResponse = {
    val profilesByUserId = profiles.map(profile => profile.userId -> profile).toMap
    ArticleListResponse(
      articles =
        for {
          article <- articles
          author  <- profilesByUserId.get(article.authorId)
        } yield Payload(
          slug = article.slug,
          title = article.title,
          description = article.description,
          tagList = article.tags,
          createdAt = article.createdAt.toString,
          updatedAt = article.updatedAt.toString,
          favoritesCount = article.favoriteCount,
          favorited = favorites.contains(article.id),
          author = GetProfileResponse.payload(author, followed.contains(author.id)),
        ),
      articlesCount = count,
    )
  }
