package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.error.NotFound.ProfileNotFound
import conduit.domain.model.UserProfile
import conduit.domain.model.UserProfile.Follower
import conduit.domain.request.user.FollowUserRequest
import conduit.domain.response.user.GetProfileResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for following a user.
 *
 * This use case handles the action of one user following another user's profile.
 * It creates a follower relationship within a database transaction and returns
 * the followed user's profile with the following flag set to true.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @tparam Tx The type of database transaction.
 */
class ProfileFollowingUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Follows a user profile based on the authenticated request.
   *
   * Executes within a database transaction to:
   * 1. Find the target user profile by username
   * 2. Create a follower relationship between the requester and target user
   * 3. Return the profile with following flag set to true
   *
   * @param request The request containing the requester and target username.
   * @return The followed user's profile response wrapped in Effect context.
   */
  def apply(request: FollowUserRequest): GetProfileResponse < Effect =
    database.transaction:
      for {
        profile <- findProfile(request)
        follower = Follower(request.requester.userId, profile.id)
        _       <- persistence.followers.add(follower)
      } yield GetProfileResponse.make(profile, true)

  /**
   * Finds a user profile by username.
   *
   * @param request The follow request containing the target username.
   * @return The user profile if found, or aborts with ProfileNotFound error.
   */
  private def findProfile(request: FollowUserRequest): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUsername(request.username)
      ?! ProfileNotFound(request.username)

}
