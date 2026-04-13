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
) {
  /**
   * Converts the Article to its core data representation, excluding aggregated fields.
   *
   * This is used when saving or updating an article, where only the content fields are relevant.
   * The repository or service layer can use this method to extract the necessary data for persistence
   * while ignoring fields like favorite count and tags that are managed separately.
   *
   * @return an Article.Data instance containing the core data of the article
   */
  def data: Article.Data =
    Article.Data(
      id = id,
      slug = slug,
      title = title,
      description = description,
      body = body,
      authorId = authorId,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
}

object Article:
  /** Type alias for article identifiers using UUID */
  type Id = UUID

  /**
   * Represents the core data of an article, excluding aggregated fields.
   *
   * This is used for creating and updating articles, where only the content fields are relevant.
   *
   * @param id the unique identifier for the article
   * @param slug the URL-friendly identifier used in routes
   * @param title the article title
   * @param description a brief summary or description of the article
   * @param body the main content of the article
   * @param authorId the unique identifier of the user who authored the article
   * @param createdAt the timestamp when the article was created
   * @param updatedAt the timestamps for article creation and last modification
   */
  case class Data(
    id: Article.Id,
    slug: String,
    title: String,
    description: String,
    body: String,
    authorId: User.Id,
    createdAt: Instant,
    updatedAt: Instant,
  ) {

    /**
     * Converts the Article.Data to a full Article domain object by adding the favorite count and tags.
     *
     * This is used when creating or updating an article, where the input is the article data without
     * the favorite count and tags. The repository or service layer is responsible for providing these
     * additional fields when constructing the complete Article object.
     *
     * @param favoriteCount the number of users who have favorited this article
     * @param tags a list of tags associated with the article for categorization
     * @return a complete Article object with all fields populated
     */
    def toArticle(favoriteCount: Int, tags: List[String]): Article =
      Article(
        id = id,
        slug = slug,
        title = title,
        description = description,
        body = body,
        authorId = authorId,
        favoriteCount = favoriteCount,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
      )
  }

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
