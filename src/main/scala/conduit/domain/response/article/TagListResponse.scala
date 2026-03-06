package conduit.domain.response.article

/**
 * Response model for retrieving a list of available tags.
 *
 * @param tags List of tag names available in the system
 */
case class TagListResponse(tags: List[String])
