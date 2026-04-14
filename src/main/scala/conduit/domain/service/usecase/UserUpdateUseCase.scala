package conduit.domain.service.usecase

import conduit.domain.error.{ ApplicationError, CredentialsInvalidInput, ProfileInvalidInput }
import conduit.domain.error.MissingEntity.{ CredentialsMissing, UserProfileMissing }
import conduit.domain.model.{ Credentials, User, UserProfile }
import conduit.domain.request.Patchable
import conduit.domain.request.user.UpdateUserRequest
import conduit.domain.request.user.UpdateUserRequest.Patch
import conduit.domain.response.user.{ AuthenticationResponse, GetProfileResponse }
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.service.validation.{ CredentialsInputValidation, StateValidationService, UserProfileInputValidation }
import conduit.domain.syntax.*
import kyo.*
import zio.prelude.Validation

/**
 * Use case for updating user profile and credentials.
 *
 * This use case provides methods to update a user's profile information and credentials
 * (such as email, password, username, bio, and image) based on the provided patches.
 * All updates are performed within a database transaction, and validation is applied
 * to ensure uniqueness constraints and data integrity.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @param authentication The authentication service for hashing passwords.
 * @param stateValidation The state validation service for checking uniqueness constraints.
 * @tparam Tx The type of database transaction.
 */
class UserUpdateUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
  authentication: AuthenticationService,
  stateValidation: StateValidationService[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Updates a user's profile and credentials based on the request.
   *
   * Executes within a database transaction to:
   * 1. Parse and validate all requested changes
   * 2. Update the user profile with valid patches
   * 3. Update the user credentials with valid patches
   * 4. Return the updated profile response with following flag set to false
   *
   * @param request The update request containing the requester and update data.
   * @return The updated user profile response wrapped in Effect context.
   */
  def apply(request: UpdateUserRequest): AuthenticationResponse < Effect =
    database.transaction:
      for {
        patches <- parse(request)
        profile <- updateUserProfile(request, patches)
        creds   <- updateUserCredentials(request, patches)
        token   <- authentication.encodeToken(request.requester.userId)
      } yield AuthenticationResponse.make(creds.email, profile, token)

  /**
   * Updates only the user profile based on the provided patches.
   *
   * Finds the current profile, applies the patches, and saves the updated profile
   * only if changes were made. If the profile remains unchanged, no database update occurs.
   *
   * @param request The update request containing the requester.
   * @param patches The list of validated patches to apply.
   * @return The updated user profile wrapped in Effect context.
   */
  private def updateUserProfile(request: UpdateUserRequest, patches: List[Patch]): UserProfile < (Effect & Env[Tx]) =
    for {
      profile <- findProfile(request)
      patched <- patch(profile, patches)
      _       <- if profile == patched then Kyo.unit
                 else persistence.users.update(patched)
    } yield patched

  /**
   * Updates only the user credentials based on the provided patches.
   *
   * Finds the current credentials, applies the patches, and saves the updated credentials
   * only if changes were made. If neither email nor password is provided in the request,
   * or if credentials remain unchanged, no database update occurs.
   *
   * @param request The update request containing the requester.
   * @param patches The list of validated patches to apply.
   * @return Unit wrapped in Effect context.
   */
  private def updateUserCredentials(request: UpdateUserRequest, patches: List[Patch]): Credentials < (Effect & Env[Tx]) =
    val payload = request.payload.user
    if payload.email == Patchable.Absent && payload.password == Patchable.Absent then findCredentials(request)
    else
      for {
        creds   <- findCredentials(request)
        patched <- patch(creds, patches)
        _       <- if creds == patched then Kyo.unit
                   else persistence.credentials.update(request.requester.userId, patched)
      } yield patched

  /**
   * Finds the user profile for the authenticated user.
   *
   * @param request The update request containing the requester.
   * @return The user profile if found, or aborts with UserProfileMissing error.
   */
  private def findProfile(request: UpdateUserRequest): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUser(request.requester.userId)
      ?! UserProfileMissing(request.requester.userId)

  /**
   * Finds the credentials for the authenticated user.
   *
   * @param request The update request containing the requester.
   * @return The hashed credentials if found, or aborts with CredentialsMissing error.
   */
  private def findCredentials(request: UpdateUserRequest): Credentials.Hashed < (Effect & Env[Tx]) =
    persistence.credentials.find(request.requester.userId)
      ?! CredentialsMissing(request.requester.userId)

  /**
   * Parses and validates all update patches from the request.
   *
   * Validates each provided field (email, password, bio, username, image) and
   * collects all valid patches. Returns a validation error if any field fails validation.
   *
   * @param request The update request containing optional fields to update.
   * @return A list of valid patches or validation errors wrapped in Effect context.
   */
  private def parse(request: UpdateUserRequest): List[Patch] < (Effect & Env[Tx]) = {
    for {
      emailPatch    <- parseEmail(request)
      passwordPatch <- parsePassword(request)
      bioPatch      <- parseBio(request)
      namePatch     <- parseName(request)
      imagePatch    <- parseImage(request)
    } yield Validation.validateAll {
      List.from(emailPatch ++ passwordPatch ++ bioPatch ++ namePatch ++ imagePatch)
    }
  }.validOrAbort

  /**
   * Parses and validates the email field from the request.
   *
   * Validates the email format and checks that it's not already in use by another user.
   *
   * @param request The update request.
   * @return An optional validated Email patch, or None if email is not provided.
   */
  private def parseEmail(request: UpdateUserRequest): Option[Validated[Patch]] < (Effect & Env[Tx]) =
    request.payload.user.email match
      case Patchable.Absent         => None
      case Patchable.Emtpy          => Some(Validation.fail(CredentialsInvalidInput.EmptyEmail))
      case Patchable.Present(email) =>
        CredentialsInputValidation.email(email).flatTraverse(stateValidation.validateEmailIsFree).map {
          case Validation.Success(_, validEmail) => Some(Validation.succeed(Patch.Email(validEmail)))
          case Validation.Failure(logs, errors)  => Some(Validation.Failure(logs, errors))
        }

  /**
   * Parses and validates the password field from the request.
   *
   * Validates the password format and hashes it using the authentication service.
   *
   * @param request The update request.
   * @return An optional validated Password patch, or None if password is not provided.
   */
  private def parsePassword(request: UpdateUserRequest): Option[Validated[Patch]] < Effect =
    request.payload.user.password match
      case Patchable.Absent            => None
      case Patchable.Emtpy             => Some(Validation.fail(CredentialsInvalidInput.EmptyPassword))
      case Patchable.Present(password) =>
        CredentialsInputValidation.password(password).traverse(authentication.hashPassword).map {
          case Validation.Success(_, pwd)       => Some(Validation.succeed(Patch.Password(pwd)))
          case Validation.Failure(logs, errors) => Some(Validation.Failure(logs, errors))
        }

  /**
   * Parses and validates the bio field from the request.
   *
   * Trims the bio and converts empty strings to Emtpy. Validates non-empty bios.
   *
   * @param request The update request.
   * @return An optional validated Bio patch, or None if bio is not provided.
   */
  private def parseBio(request: UpdateUserRequest): Option[Validated[Patch]] < Any =
    request.payload.user.bio match
      case Patchable.Absent       => None
      case Patchable.Emtpy        => Some(Validation.succeed(Patch.Bio(Maybe.Absent)))
      case Patchable.Present(raw) =>
        raw.trim match
          case ""  => Some(Validation.succeed(Patch.Bio(Maybe.Absent)))
          case bio => Some(UserProfileInputValidation.bio(bio).map(bio => Patch.Bio(Maybe.Present(bio))))

  /**
   * Parses and validates the username field from the request.
   *
   * Validates the username format and checks that it's not already in use by another user.
   *
   * @param request The update request.
   * @return An optional validated Username patch, or None if username is not provided.
   */
  private def parseName(request: UpdateUserRequest): Option[Validated[Patch]] < (Effect & Env[Tx]) =
    request.payload.user.username match
      case Patchable.Absent        => None
      case Patchable.Emtpy         => Some(Validation.fail(ProfileInvalidInput.EmptyUsername))
      case Patchable.Present(name) =>
        UserProfileInputValidation.name(name).flatTraverse(stateValidation.validateUsernameIsFree).map {
          case Validation.Success(_, validName) => Some(Validation.succeed(Patch.Username(validName)))
          case Validation.Failure(logs, errors) => Some(Validation.Failure(logs, errors))
        }

  /**
   * Parses and validates the image field from the request.
   *
   * Validates the image URI format.
   *
   * @param request The update request.
   * @return An optional validated Image patch, or None if image is not provided.
   */
  private def parseImage(request: UpdateUserRequest): Option[Validated[Patch]] < Any =
    request.payload.user.image match
      case Patchable.Absent       => None
      case Patchable.Emtpy        => Some(Validation.succeed(Patch.Image(Maybe.Absent)))
      case Patchable.Present(raw) =>
        raw.trim match
          case ""  => Some(Validation.succeed(Patch.Image(Maybe.Absent)))
          case uri =>
            UserProfileInputValidation.image(uri) match {
              case Validation.Success(_, validUri)  => Some(Validation.succeed(Patch.Image(Maybe.Present(validUri))))
              case Validation.Failure(logs, errors) => Some(Validation.Failure(logs, errors))
            }

  /**
   * Applies a list of patches to a user profile.
   *
   * Updates the profile fields based on the provided patches (username, bio, image).
   *
   * @param profile The current user profile.
   * @param patches The list of patches to apply.
   * @return The updated user profile.
   */
  private def patch(profile: UserProfile, patches: List[Patch]): UserProfile < Any =
    patches.foldLeft(profile) {
      case profile -> Patch.Username(username) => profile.copy(name = username)
      case profile -> Patch.Bio(bio)           => profile.copy(biography = bio)
      case profile -> Patch.Image(image)       => profile.copy(image = image)
      case profile -> _                        => profile
    }

  /**
   * Applies a list of patches to user credentials.
   *
   * Updates the credentials based on the provided patches (email, password).
   *
   * @param credentials The current hashed credentials.
   * @param patches The list of patches to apply.
   * @return The updated credentials.
   */
  private def patch(credentials: Credentials.Hashed, patches: List[Patch]): Credentials.Hashed < Any =
    patches.foldLeft(credentials) {
      case creds -> Patch.Email(email)       => creds.copy(email = email)
      case creds -> Patch.Password(password) => creds.copy(password = password)
      case creds -> _                        => creds
    }
}
