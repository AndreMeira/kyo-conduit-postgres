package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.request.article.ListTagsRequest
import conduit.domain.response.article.TagListResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import kyo.*

/**
 * Use case for listing all available tags.
 *
 * Retrieves the complete list of tags used across all articles. This is a
 * public endpoint that does not require authentication.
 *
 * @param database    the database abstraction for managing transactions
 * @param persistence the persistence layer providing access to repositories
 * @tparam Tx the database transaction type
 */
class ListTagsUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Retrieves all tags.
   *
   * @param request the list tags request
   * @return a response containing all available tags
   */
  def apply(request: ListTagsRequest): TagListResponse < Effect =
    database.transaction:
      for tags <- persistence.tags.findAll
      yield TagListResponse(tags)
}
