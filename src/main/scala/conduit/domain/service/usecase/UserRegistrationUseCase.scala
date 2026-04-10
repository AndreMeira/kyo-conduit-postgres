package conduit.domain.service.usecase

import conduit.domain.syntax.*
import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Credentials, UserProfile }
import conduit.domain.request.user.RegistrationRequest
import conduit.domain.response.user.GetProfileResponse
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.Database.Transaction
import conduit.domain.service.persistence.{ Database, IdGeneratorService, Persistence }
import conduit.domain.service.validation.{ CredentialsInputValidation, StateValidationService, UserProfileInputValidation }
import kyo.*
import zio.prelude.Validation

import java.util.UUID

/**
 * Use case for registering a new user.
 *
 * This use case handles user registration by validating credentials and username,
 * creating a user profile, and storing both the profile and hashed credentials
 * within a database transaction. It ensures email and username uniqueness through
 * state validation.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @param authentication The authentication service for hashing passwords.
 * @param stateValidation The state validation service for checking uniqueness constraints.
 * @tparam Tx The type of database transaction.
 */
class UserRegistrationUseCase[Tx <: Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
  authentication: AuthenticationService,
  stateValidation: StateValidationService[Tx],
) {
  type Effect = Async & Abort[ApplicationError]

  /**
   * Registers a new user based on the registration request.
   *
   * Executes within a database transaction to:
   * 1. Parse and validate the registration data (email, username, password)
   * 2. Hash the user's password
   * 3. Create a new user profile
   * 4. Save the profile and credentials to the database
   * 5. Return the profile response with following flag set to false
   *
   * @param request The registration request containing user data.
   * @return The newly created user profile response wrapped in Effect context.
   */
  def apply(request: RegistrationRequest): GetProfileResponse < Effect =
    database.transaction:
      for {
        (name, creds) <- parse(request).validOrAbort
        hashedCreds   <- authentication.hashed(creds)
        profile       <- createProfile(name)
        _             <- persistence.credentials.save(profile.userId, hashedCreds)
        _             <- persistence.users.save(profile)
      } yield GetProfileResponse.make(profile, false)

  /**
   * Parses and validates the registration request data.
   *
   * Validates the email format and uniqueness, password format, and username
   * format and uniqueness. Returns a tuple of username and clear credentials
   * if all validations pass.
   *
   * @param request The registration request containing user input.
   * @return A validated tuple of username and credentials, or validation errors.
   */
  private def parse(request: RegistrationRequest): Validated[(String, Credentials)] < (Effect & Env[Tx]) =
    for {
      email    <- CredentialsInputValidation.email(request.payload.user.email).lift
      email    <- email.flatTraverse(stateValidation.validateEmailIsFree)
      password <- CredentialsInputValidation.password(request.payload.user.password).lift
      name     <- UserProfileInputValidation.name(request.payload.user.username).lift
      name     <- name.flatTraverse(stateValidation.validateUsernameIsFree)
    } yield Validation.validateWith(name, email, password) { (name, email, password) =>
      name -> Credentials.Clear(email, password)
    }

  /**
   * Creates a new user profile with generated IDs and timestamps.
   *
   * Generates unique UUIDs for the user ID and profile ID, and uses the current
   * timestamp for creation and update times.
   *
   * @param name The username for the new profile.
   * @return The newly created user profile wrapped in Effect context.
   */
  private def createProfile(name: String): UserProfile < (Effect & Env[Tx]) =
    for {
      now           <- Clock.now.map(_.toJava)
      userId        <- IdGeneratorService.uuid
      userProfileId <- IdGeneratorService.uuid
    } yield UserProfile(userProfileId, userId, name, Maybe.Absent, Maybe.Absent, now, now)

}
