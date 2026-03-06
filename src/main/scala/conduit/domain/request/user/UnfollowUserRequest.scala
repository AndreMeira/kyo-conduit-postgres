package conduit.domain.request.user

import conduit.domain.model.User

/**
 * Represents a request to unfollow a user in the Conduit application.
 *
 * This request is made by an authenticated user who wants to unfollow another user
 * identified by their username.
 *
 * @param requester the authenticated user making the request
 * @param username the username of the user to be unfollowed
 */
case class UnfollowUserRequest(
  requester: User.Authenticated,
  username: String,
)
