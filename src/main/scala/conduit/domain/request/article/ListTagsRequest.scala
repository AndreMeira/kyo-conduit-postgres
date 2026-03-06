package conduit.domain.request.article

import conduit.domain.model.User

/**
 * Represents a request to list all tags available in the Conduit application.
 *
 * This request is made by a user (authenticated or unauthenticated) who wants to
 * retrieve the list of tags used to categorize articles.
 *
 * @param requester the user making the request
 */
case class ListTagsRequest(requester: User)
