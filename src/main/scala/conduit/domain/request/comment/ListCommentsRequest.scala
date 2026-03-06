package conduit.domain.request.comment

import conduit.domain.model.User

/**
 * Represents a request to list comments for a specific article in the Conduit application.
 *
 * This request is used when a user wants to retrieve all comments associated with a particular article.
 * The requester can be either an authenticated or unauthenticated user, and must provide the slug
 * of the article for which comments are to be listed.
 *
 * @param requester the user making the request, can be authenticated or unauthenticated
 * @param slug the URL-friendly identifier of the article whose comments are to be listed
 */
case class ListCommentsRequest(requester: User, slug: String)
