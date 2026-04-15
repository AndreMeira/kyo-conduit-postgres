package conduit.domain.service.validation

import conduit.domain.error.ProfileInvalidInput.{ BiographyIsEmpty, EmptyUsername, InvalidImageUri, UserNameInvalidChar, UserNameLengthViolation }
import conduit.domain.syntax.Validated
import conduit.domain.types.*
import zio.prelude.Validation

import java.net.URI
import scala.util.Try

/**
 * Provides validation logic for user profile requests, including validation for name, bio, and image fields.
 */
object UserProfileInputValidation {
  private val allowedChars = "^[a-zA-Z0-9_]+$".r

  /**
   * Validates the username.
   *
   * @param value The username to validate.
   * @return A validated username or an error indicating the violation.
   */
  def name(value: String): Validated[ProfileName] =
    Validation
      .validate(
        CommonValidation
          .nonEmptyString(value)
          .asError(EmptyUsername),
        CommonValidation
          .length(value, 3, 30)
          .asError(UserNameLengthViolation(value, 3, 30)),
        CommonValidation
          .nonEmptyMatch(value, allowedChars)
          .asError(UserNameInvalidChar(allowedChars)),
      )
      .flatMap(CommonValidation.sameValues(_, _, _))
      .map(ProfileName.apply)

  /**
   * Validates the biography field.
   *
   * @param value The biography to validate.
   * @return A validated biography or an error if the biography is empty.
   */
  def bio(value: String): Validated[ProfileBiography] =
    CommonValidation
      .nonEmptyString(value.trim)
      .asError(BiographyIsEmpty)
      .map(ProfileBiography.apply)

  /**
   * Validates the image URI.
   *
   * @param value The image URI as a string.
   * @return A validated URI or an error if the URI is invalid.
   */
  def image(value: String): Validated[ProfileImage] =
    Try(URI.create(value.trim)).toEither match {
      case Right(uri) => Validation.succeed(ProfileImage(uri))
      case Left(_)    => Validation.fail(InvalidImageUri(value))
    }
}
