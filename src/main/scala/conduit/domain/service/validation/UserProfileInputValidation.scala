package conduit.domain.service.validation

import conduit.domain.request.user.InvalidInput.{ BiographyIsEmpty, InvalidImageUri, UserNameInvalidChar, UserNameLengthViolation }
import conduit.domain.syntax.Validated
import zio.prelude.Validation

import java.net.URI
import scala.util.Try

/**
 * Provides validation logic for user profile requests, including validation for name, bio, and image fields.
 */
object UserProfileInputValidation {
  private val allowedChars = "^[a-zA-Z0-9]+$".r

  /**
   * Validates the username.
   *
   * @param value The username to validate.
   * @return A validated username or an error indicating the violation.
   */
  def name(value: String): Validated[String] =
    Validation
      .validate(
        CommonValidation
          .length(value, 3, 30)
          .asError(UserNameLengthViolation(value, 3, 30)),
        CommonValidation
          .nonEmptyMatch(value, allowedChars)
          .asError(UserNameInvalidChar(allowedChars)),
      )
      .flatMap(CommonValidation.sameValues(_, _))

  /**
   * Validates the biography field.
   *
   * @param value The biography to validate.
   * @return A validated biography or an error if the biography is empty.
   */
  def bio(value: String): Validated[String] =
    CommonValidation
      .nonEmptyString(value.trim)
      .asError(BiographyIsEmpty)

  /**
   * Validates the image URI.
   *
   * @param value The image URI as a string.
   * @return A validated URI or an error if the URI is invalid.
   */
  def image(value: String): Validated[URI] =
    Try(URI.create(value.trim)).toEither match {
      case Right(uri) => Validation.succeed(uri)
      case Left(_)    => Validation.fail(InvalidImageUri(value))
    }
}
