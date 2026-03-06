package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.error.NotFound.ProfileNotFound
import conduit.domain.model.UserProfile.FollowedBy
import conduit.domain.model.{ User, UserProfile }
import conduit.domain.request.user.GetProfileRequest
import conduit.domain.response.user.GetProfileResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for reading a user's profile.
 *
 * This use case handles the logic for retrieving a user's profile information,
 * including whether the requesting user is following the profile being viewed.
 *
 * @param database the database service for managing transactions
 * @param persistence the persistence service for accessing user data
 * @tparam Tx the type of database transaction
 */
class ProfileReadUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Retrieves a user's profile based on the provided request.
   *
   * @param request the request containing the requester and the username of the profile to retrieve
   * @return the response containing the user's profile and following status
   */
  def apply(request: GetProfileRequest): GetProfileResponse < Effect =
    database.transaction:
      for {
        profile   <- findProfile(request)
        following <- isFollowing(request.requester, profile)
      } yield GetProfileResponse.make(profile, following)

  /**
   * Finds the user profile by username.
   *
   * @param request the request containing the username to search for
   * @return the user profile if found, otherwise raises a ProfileNotFound error
   */
  private def findProfile(request: GetProfileRequest): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUsername(request.username)
      ?! ProfileNotFound(request.username)

  /**
   * Checks if the requesting user is following the specified profile.
   *
   * @param user the requesting user
   * @param profile the user profile to check against
   * @return true if the user is following the profile, false otherwise
   */
  private def isFollowing(user: User, profile: UserProfile): Boolean < (Effect & Env[Tx]) =
    user.option match
      case Maybe.Absent          => false
      case Maybe.Present(userId) => persistence.followers.exists(FollowedBy(userId, profile.id))
}
