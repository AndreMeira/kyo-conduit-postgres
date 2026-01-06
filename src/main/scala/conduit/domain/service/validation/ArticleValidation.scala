package conduit.domain.service.validation

import conduit.domain.error.ArticleInvalidInput as Invalid
import conduit.domain.syntax.Validated
import zio.prelude.Validation

import java.util.UUID
import scala.util.chaining.scalaUtilChainingOps

/**
 * Validation service for article-related input data.
 *
 * This object provides validation functions for all article fields, ensuring
 * that article data conforms to business rules before being processed by
 * domain services. Each validation function returns a Validated type that
 * can accumulate multiple validation errors.
 */
object ArticleValidation {

  /**
   * Validates an article ID string and converts it to UUID.
   *
   * @param value the string representation of the article ID
   * @return a validated UUID or InvalidId error
   */
  def id(value: String): Validated[UUID] =
    CommonValidation
      .uuid(value)
      .asError(Invalid.InvalidId)

  /**
   * Validates an article title is non-empty.
   *
   * @param value the article title to validate
   * @return a validated title string or EmptyTitle error
   */
  def title(value: String): Validated[String] =
    CommonValidation
      .nonEmptyString(value)
      .asError(Invalid.EmptyTitle)

  /**
   * Validates an article description is non-empty.
   *
   * @param value the article description to validate
   * @return a validated description string or EmptyDescription error
   */
  def description(value: String): Validated[String] =
    CommonValidation
      .nonEmptyString(value)
      .asError(Invalid.EmptyDescription)

  /**
   * Validates an article body is non-empty.
   *
   * @param value the article body to validate
   * @return a validated body string or EmptyBody error
   */
  def body(value: String): Validated[String] =
    CommonValidation
      .nonEmptyString(value)
      .asError(Invalid.EmptyBody)

  /**
   * Validates an author ID string and converts it to UUID.
   *
   * @param value the string representation of the author ID
   * @return a validated UUID or InvalidId error
   */
  def authorId(value: String): Validated[UUID] =
    CommonValidation
      .uuid(value)
      .asError(Invalid.InvalidId)

  /**
   * Validates that favorite count is positive.
   *
   * @param value the favorite count to validate
   * @return a validated positive integer or CountMustBePositive error
   */
  def favoriteCount(value: Int): Validated[Int] =
    CommonValidation
      .positive(value)
      .asError(Invalid.CountMustBePositive)

  /**
   * Validates a list of tags, ensuring each tag is non-empty.
   *
   * @param tagList the list of tag strings to validate
   * @return a validated list of non-empty tags or EmptyTag errors
   */
  def tags(tagList: List[String]): Validated[List[String]] =
    tagList
      .map(CommonValidation.nonEmptyString(_).asError(Invalid.EmtpyTag))
      .pipe(Validation.validateAll(_))
}
