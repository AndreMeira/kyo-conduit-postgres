package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.model.{ User, UserProfile }
import conduit.domain.request.user.GetUserRequest
import conduit.domain.response.user.{ AuthenticationResponse, GetProfileResponse }
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for reading user profile information.
 *
 * This use case handles retrieving a user's profile based on an authenticated request.
 * It performs the operation within a database transaction and returns the user's profile
 * information wrapped in a response.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @tparam Tx The type of database transaction.
 */
class UserReadUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
  authentication: AuthenticationService,
) {

  private type Effect = Async & Abort[ApplicationError]

  /**
   * Retrieves the user profile for the authenticated requester.
   *
   * Executes within a database transaction to find the user's profile and returns
   * it wrapped in a GetProfileResponse with the following flag set to false.
   *
   * @param request The request containing the authenticated user information.
   * @return The user profile response wrapped in Effect context.
   */
  def apply(request: GetUserRequest): AuthenticationResponse < Effect =
    database.transaction:
      for {
        profile <- findProfile(request.requester)
        creds   <- persistence.credentials.find(request.requester.userId)
                     ?! UserProfileMissing(request.requester.userId) 
        token   <- authentication.encodeToken(request.requester.userId)
      } yield AuthenticationResponse.make(creds.email, profile, token)

  /**
   * Finds a user profile by the authenticated user's ID.
   *
   * @param user The authenticated user whose profile should be retrieved.
   * @return The user profile if found, or aborts with UserProfileMissing error.
   */
  private def findProfile(user: User.Authenticated): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUser(user.userId)
      ?! UserProfileMissing(user.userId)
}
