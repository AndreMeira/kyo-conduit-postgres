package conduit.domain.request.article

import conduit.domain.model.User

/**
 * Represents a request to create a new article in the Conduit application.
 *
 * This request is used when authenticated users want to publish new articles.
 * The requester must be authenticated and provide the article content including
 * title, description, body, and optional tags.
 *
 * @param requester must be an authenticated user to create articles
 * @param payload the article data wrapped according to API specification
 */
case class CreateArticleRequest(
  requester: User.Authenticated,
  payload: CreateArticleRequest.Payload,
)

object CreateArticleRequest:
  /**
   * Wrapper for article data to match API specification format.
   *
   * @param article the actual article data for creation
   */
  case class Payload(article: Data) // Wraps the article data due to API spec

  /**
   * Contains the article data fields for creation.
   *
   * @param title the article title
   * @param description a brief summary of the article
   * @param body the main content of the article
   * @param tagList optional list of tags for categorization
   */
  case class Data(
    title: String,
    description: String,
    body: String,
    tagList: Option[List[String]], // @todo replace with kyo.Maybe
  ) {

    /** Returns tags as a List, defaulting to empty list if not provided */
    def tags: List[String] = tagList.getOrElse(Nil)
  }
