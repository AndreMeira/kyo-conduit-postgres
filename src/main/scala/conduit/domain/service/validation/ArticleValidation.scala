package conduit.domain.service.validation

import conduit.domain.error.ArticleInvalidInput as Invalid
import conduit.domain.syntax.Validated
import zio.prelude.Validation

import java.util.UUID
import scala.util.chaining.scalaUtilChainingOps

object ArticleValidation {

  def id(value: String): Validated[UUID] =
    CommonValidation
      .uuid(value)
      .asError(Invalid.InvalidId)

  def title(value: String): Validated[String] =
    CommonValidation
      .nonEmptyString(value)
      .asError(Invalid.EmptyTitle)

  def description(value: String): Validated[String] =
    CommonValidation
      .nonEmptyString(value)
      .asError(Invalid.EmptyDescription)

  def body(value: String): Validated[String] =
    CommonValidation
      .nonEmptyString(value)
      .asError(Invalid.EmptyBody)

  def authorId(value: String): Validated[UUID] =
    CommonValidation
      .uuid(value)
      .asError(Invalid.InvalidId)

  def favoriteCount(value: Int): Validated[Int] =
    CommonValidation
      .positive(value)
      .asError(Invalid.CountMustBePositive)

  def tags(tagList: List[String]): Validated[List[String]] =
    tagList
      .map(CommonValidation.nonEmptyString(_).asError(Invalid.EmtpyTag))
      .pipe(Validation.validateAll(_))
}
