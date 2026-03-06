package conduit.domain.request.user

import conduit.domain.model.User

/**
 * Represents a request to retrieve a user's profile in the Conduit application.
 *
 * This request is made by a user (authenticated or unauthenticated) who wants to
 * view the profile of another user identified by their username.
 *
 * @param requester the user making the request
 * @param username the username of the user whose profile is being requested
 */
case class GetProfileRequest(
  requester: User,
  username: String,
)
