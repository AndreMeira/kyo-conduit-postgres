package conduit.domain.service.validation

import conduit.domain.error.CommentInvalidInput as Invalid
import conduit.domain.syntax.Validated
import conduit.domain.types.*

/**
 * Validation for comment-related input data.
 *
 * This object provides validation functions for comment fields, ensuring
 * that comment data conforms to business rules before being processed by
 * domain services. Each validation function returns a Validated type that
 * can accumulate multiple validation errors.
 */
object CommentInputValidation {

  /**
   * Validates a comment ID is positive.
   *
   * @param value the comment ID to validate
   * @return a validated positive Long ID or IdIsNotPositive error
   */
  def id(value: Long): Validated[CommentId] =
    CommonValidation
      .positive(value)
      .asError(Invalid.IdIsNotPositive(CommentId(value)))
      .map(CommentId.apply)

  /**
   * Validates a comment body is non-empty.
   *
   * @param value the comment body text to validate
   * @return a validated body string or EmptyBody error
   */
  def body(value: String): Validated[CommentBody] =
    CommonValidation
      .nonEmptyString(value)
      .asError(Invalid.EmptyBody)
      .map(CommentBody.apply)
}
