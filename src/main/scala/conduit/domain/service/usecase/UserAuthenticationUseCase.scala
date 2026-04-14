package conduit.domain.service.usecase

import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.error.{ ApplicationError, Unauthorised }
import conduit.domain.model.{ Credentials, User }
import conduit.domain.request.user.AuthenticateRequest
import conduit.domain.response.user.AuthenticationResponse
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.Database.Transaction
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.service.validation.{ CredentialsInputValidation, StateValidationService }
import zio.prelude.Validation
import conduit.domain.syntax.*
import kyo.*

/**
 * Use case for authenticating a user.
 *
 * This use case handles the authentication process by validating the input credentials,
 * hashing the password, checking the credentials against the database, and generating
 * an authentication token if successful. It also retrieves the user's profile information
 * to include in the response.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @param authentication The authentication service for hashing and token generation.
 * @tparam Tx The type of database transaction.
 */
class UserAuthenticationUseCase[Tx <: Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
  authentication: AuthenticationService,
) {
  type Effect = Async & Abort[ApplicationError]

  /**
   * Authenticates a user based on the provided credentials in the request.
   *
   * The authentication process includes:
   * 1. Validating the input credentials (email and password).
   * 2. Hashing the password using the authentication service.
   * 3. Checking the hashed credentials against the database to find the user ID.
   * 4. Generating an authentication token for the user.
   * 5. Retrieving the user's profile information to include in the response.
   *
   * @param request The authentication request containing user credentials.
   * @return An AuthenticationResponse containing user profile and token if successful, or an error if authentication fails.
   */
  def apply(request: AuthenticateRequest): AuthenticationResponse < Effect =
    database.transaction:
      for {
        credentials <- parse(request).validOrAbort
        hashed      <- authentication.hash(credentials)
        userId      <- authenticate(hashed)
        token       <- authentication.encodeToken(userId)
        profile     <- persistence.users.findByUser(userId) ?! UserProfileMissing(userId)
      } yield AuthenticationResponse.make(credentials.email, profile, token)

  /**
   * Parses and validates the authentication request credentials.
   *
   * This function validates the email and password fields of the request using the CredentialsInputValidation.
   * If validation is successful, it returns a Credentials.Clear instance containing the validated email and password.
   * If validation fails, it accumulates the validation errors and returns them in the Effect context.
   *
   * @param request The authentication request containing user credentials.
   * @return A validated Credentials.Clear instance or validation errors wrapped in Effect context.
   */
  private def parse(request: AuthenticateRequest): Validated[Credentials.Clear] < Any =
    Kyo.lift {
      Validation.validateWith(
        CredentialsInputValidation.email(request.payload.user.email),
        CredentialsInputValidation.password(request.payload.user.password),
      )(Credentials.Clear(_, _))
    }

  /**
   * Authenticates the user by checking the hashed credentials against the database.
   *
   * This function queries the persistence layer to find a user ID that matches the provided hashed credentials.
   * If a matching user ID is found, it returns the user ID. If no match is found, it fails with an Unauthorised.InvalidCredentials error.
   *
   * @param credentials The hashed credentials to authenticate.
   * @return The user ID if authentication is successful, or an error if authentication fails.
   */
  private def authenticate(credentials: Credentials.Hashed): User.Id < (Effect & Env[Tx]) =
    persistence.credentials.find(credentials).map {
      case Maybe.Present(userId) => userId
      case Maybe.Absent          => Abort.fail(Unauthorised.InvalidCredentials)
    }
}
