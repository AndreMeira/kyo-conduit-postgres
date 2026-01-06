package conduit.domain.service.validation

import conduit.domain.error.CommentInvalidInput as Invalid
import conduit.domain.syntax.Validated

object CommentValidation {

  def id(value: Long): Validated[Long] =
    CommonValidation
      .positive(value)
      .asError(Invalid.IdIsNotPositive(value))

  def body(value: String): Validated[String] =
    CommonValidation
      .nonEmptyString(value)
      .asError(Invalid.EmptyBody)
}
