package conduit.domain.service.validation

import zio.prelude.Validation
import conduit.domain.error.ApplicationError
import conduit.domain.request.user.InvalidCredentialsInput.EmailAlreadyInUse
import conduit.domain.request.user.InvalidInput.UsernameAlreadyExists
import conduit.domain.model.Credentials
import conduit.domain.service.persistence.Persistence
import conduit.domain.service.persistence.Database.Transaction
import conduit.domain.syntax.*
import kyo.*

/**
 * Service for validating the state of the application, such as ensuring unique emails and usernames.
 *
 * @param persistence The persistence layer used to check the existence of entities.
 * @tparam Tx The transaction type used for database operations.
 */
class StateValidationService[Tx <: Transaction](persistence: Persistence[Tx]) {
  private type Effect = Async & Env[Tx] & Abort[ApplicationError]

  /**
   * Validates that the provided email is not already in use.
   *
   * @param email The email to validate.
   * @return A validated email if it is not in use, or an error if it is already in use.
   */
  def validateEmailIsFree(email: Credentials.Email): Validated[Credentials.Email] < Effect =
    persistence.credentials.exists(email).map {
      case true  => Validation.fail(EmailAlreadyInUse(email))
      case false => Validation.succeed(email)
    }

  /**
   * Validates that the provided email exists in the system.
   *
   * @param email The email to validate.
   * @return A validated email if it exists, or an error if it does not exist.
   */
  def validateEmailExists(email: Credentials.Email): Validated[Credentials.Email] < Effect =
    persistence.credentials.exists(email).map {
      case true  => Validation.succeed(email)
      case false => Validation.fail(EmailAlreadyInUse(email))
    }

  /**
   * Validates that the provided username is not already in use.
   *
   * @param username The username to validate.
   * @return A validated username if it is not in use, or an error if it is already in use.
   */
  def validateUsernameIsFree(username: String): Validated[String] < Effect =
    persistence.users.exists(username).map {
      case true  => Validation.fail(UsernameAlreadyExists(username))
      case false => Validation.succeed(username)
    }
}
