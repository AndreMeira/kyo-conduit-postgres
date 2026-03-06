package conduit.domain.service.persistence

import conduit.domain.error.ApplicationError
import conduit.domain.model.Article
import conduit.domain.service.persistence.Database.Transaction
import kyo.*

/**
 * Repository trait for managing article tags.
 *
 * This trait defines the contract for storing and retrieving tags associated with articles,
 * allowing different implementations (in-memory, database, etc.) to provide persistent
 * storage for article tags in the Conduit application.
 *
 * Tags are used to categorize and organize articles, enabling users to discover content
 * based on topics of interest.
 *
 * @tparam Tx the transaction type used for database operations
 */
trait TagRepository[Tx <: Transaction] {
  type Effect = Async & Env[Tx] & Abort[ApplicationError]

  /**
   * Retrieves all tags in the system.
   *
   * Returns a complete list of all unique tags that have been added to articles.
   * Useful for displaying available tags to users or for tag analytics.
   *
   * @return a list of all tag strings
   */
  def findAll: List[String] < Effect

  /**
   * Associates one or more tags with an article.
   *
   * Adds a list of tags to an article, creating tag-article relationships.
   * If a tag already exists for the article, it may be treated as a duplicate
   * depending on the implementation.
   *
   * @param articleId the ID of the article to add tags to
   * @param tag the list of tag strings to associate with the article
   * @return Unit on successful addition
   */
  def add(articleId: Article.Id, tag: List[String]): Unit < Effect

  /**
   * Retrieves all tags associated with a specific article.
   *
   * @param articleId the ID of the article to retrieve tags for
   * @return a list of tag strings associated with the specified article
   */
  def find(articleId: Article.Id): List[String] < Effect

  /**
   * Removes all tags associated with an article.
   *
   * Deletes all tag-article relationships for the specified article.
   * After this operation, the article will have no associated tags.
   *
   * @param articleId the ID of the article whose tags should be deleted
   * @return Unit on successful deletion
   */
  def delete(articleId: Article.Id, tags: List[String]): Unit < Effect
}
