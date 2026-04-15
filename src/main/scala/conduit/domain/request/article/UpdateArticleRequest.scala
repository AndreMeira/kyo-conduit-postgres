package conduit.domain.request.article

import conduit.domain.model.{ Article, User }
import conduit.domain.request.Patchable
import conduit.domain.types.*

/**
 * Represents a request to update an article in the Conduit application.
 *
 * This request is used when an authenticated user wants to update an article
 * they have authored. The requester must be authenticated and provide the
 * slug of the article to be updated along with the new article data.
 *
 * @param requester the authenticated user making the request
 * @param slug the URL-friendly identifier of the article to be updated
 * @param payload the new data for the article
 */
case class UpdateArticleRequest(
  requester: User.Authenticated,
  slug: String,
  payload: UpdateArticleRequest.Payload,
)

object UpdateArticleRequest:
  /**
   * Represents the payload for updating an article.
   *
   * @param article the new data for the article
   */
  case class Payload(article: Data)

  /**
   * Represents the data for updating an article.
   *
   * @param title the new title of the article (optional)
   * @param description the new description of the article (optional)
   * @param body the new body content of the article (optional)
   */
  case class Data(
    title: Option[String],
    description: Option[String],
    body: Option[String],
    tagList: Patchable[List[String]],
  )

  /**
   * Represents a patch operation for updating specific fields of an article.
   */
  enum Patch:
    case Body(value: ArticleBody)
    case Title(value: ArticleTitle)
    case Description(value: ArticleDescription)
    case Tags(value: List[TagName])
