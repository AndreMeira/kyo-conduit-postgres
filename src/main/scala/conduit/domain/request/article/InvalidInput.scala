package conduit.domain.request.article

import conduit.domain.error.ValidationError

/**
 * Enum representing validation errors specific to article operations.
 *
 * This enum defines all possible validation errors that can occur when
 * creating or updating articles in the Conduit application. Each case
 * represents a specific validation constraint violation related to article data.
 */
enum InvalidInput extends ValidationError.InvalidInput {

  /**
   * Error indicating that an article tag is empty.
   */
  case EmtpyTag

  /**
   * Error indicating that an article body is empty.
   */
  case EmptyBody

  /**
   * Error indicating that an article ID is invalid.
   */
  case InvalidId

  /**
   * Error indicating that an article title is empty.
   */
  case EmptyTitle

  /**
   * Error indicating that an article slug is invalid.
   */
  case InvalidSlug

  /**
   * Error indicating that an article author ID is invalid.
   */
  case InvalidAuthorId

  /**
   * Error indicating that an article description is empty.
   */
  case EmptyDescription

  /**
   * Error indicating that a favorite count must be positive.
   */
  case CountMustBePositive

  /**
   * Error indicating that no article is matching this slug.
   */
  case ArticleDoesNotExists(slug: String)

  /**
   * Returns a human-readable message describing the validation error.
   *
   * @return the error message corresponding to this validation error case
   */
  override def message: String = this match
    case InvalidSlug                => "Slug is invalid"
    case InvalidId                  => "Article id is invalid"
    case InvalidAuthorId            => "Author id is invalid"
    case EmtpyTag                   => "Tag can not be empty"
    case CountMustBePositive        => "Favorite count must be positive"
    case EmptyBody                  => "The body of an article can not be empty"
    case EmptyTitle                 => "The title of an article can not be empty"
    case EmptyDescription           => "The description of an article can not be empty"
    case ArticleDoesNotExists(slug) => s"There is no article associated with $slug"
}
