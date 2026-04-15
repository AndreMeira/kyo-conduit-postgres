package conduit.domain.service.validation

import conduit.domain.error.CredentialsInvalidInput
import conduit.domain.error.CredentialsInvalidInput.{ EmptyEmail, EmptyPassword, InvalidEmailFormat, InvalidPasswordFormat }
import conduit.domain.model.Credentials
import conduit.domain.syntax.Validated
import conduit.domain.types.*
import zio.prelude.Validation

/**
 * Validation for credentials-related input data.
 *
 * This object provides validation functions for all credentials fields,
 * ensuring that credentials data conforms to business rules before being
 * processed by domain services. Each validation function returns a Validated
 * type that can accumulate multiple validation errors.
 */
object CredentialsInputValidation {

  /**
   * Validates an email address format.
   *
   * @param value the email address to validate
   * @return a validated email address or InvalidEmailFormat error
   */
  def email(value: String): Validated[Credentials.Email] =
    Validation
      .validate(
        CommonValidation
          .nonEmptyString(value)
          .asError(EmptyEmail),
        CommonValidation
          .nonEmptyMatch(value.trim, "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".r)
          .asError(InvalidEmailFormat(value)),
      )
      .flatMap(CommonValidation.sameValues(_, _))
      .map(Email.apply)

  /**
   * Validates a password format.
   *
   * @param value the password to validate
   * @return a validated password or InvalidPasswordFormat error
   */
  def password(value: String): Validated[Credentials.Password] =
    Validation
      .validate(
        CommonValidation
          .nonEmptyString(value)
          .asError(EmptyPassword),
        CommonValidation
          .length(value.trim, min = 8, max = 20) // @todo add more complex password rules later
          .asError(InvalidPasswordFormat(value, "Password must be at least 8 characters long")),
      )
      .flatMap(CommonValidation.sameValues(_, _))
      .map(Password.apply)

}
