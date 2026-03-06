package conduit.domain.response.article

/**
 * Response model for deleting an article.
 *
 * @param article Confirmation message or identifier for the deleted article
 */
case class DeleteArticleResponse(article: String)
