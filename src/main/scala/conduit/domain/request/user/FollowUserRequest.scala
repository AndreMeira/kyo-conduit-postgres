package conduit.domain.request.user

import conduit.domain.model.User

/**
 * Represents a request to follow a user in the Conduit application.
 *
 * This request is made by an authenticated user who wants to follow another user
 * identified by their username.
 *
 * @param requester the authenticated user making the request
 * @param username the username of the user to be followed
 */
case class FollowUserRequest(
  requester: User.Authenticated,
  username: String,
)
