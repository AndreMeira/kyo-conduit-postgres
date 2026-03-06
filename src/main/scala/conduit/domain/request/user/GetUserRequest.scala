package conduit.domain.request.user

import conduit.domain.model.User

/**
 * Represents a request to retrieve the details of the authenticated user in the Conduit application.
 *
 * This request is made by an authenticated user who wants to access their own user details.
 *
 * @param requester the authenticated user making the request
 */
case class GetUserRequest(requester: User.Authenticated)
