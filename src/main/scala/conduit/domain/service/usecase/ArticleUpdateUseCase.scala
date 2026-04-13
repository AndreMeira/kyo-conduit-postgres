package conduit.domain.service.usecase

import conduit.domain.error.{ ApplicationError, Unauthorised }
import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.error.NotFound.ArticleNotFound
import conduit.domain.error.Unauthorised.ArticleUpdateDenied
import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.request.article.UpdateArticleRequest
import conduit.domain.request.article.UpdateArticleRequest.Patch
import conduit.domain.response.article.GetArticleResponse
import conduit.domain.service.persistence.{ Database, IdGeneratorService, Persistence }
import conduit.domain.service.validation.ArticleInputValidation
import conduit.domain.syntax.*
import kyo.*
import zio.prelude.Validation

/**
 * Use case for updating an article.
 * Handles validation, authorization, patching, and persistence logic.
 *
 * @param database Database service for transaction management.
 * @param persistence Persistence service for accessing and updating entities.
 * @tparam Tx Database transaction type.
 */
class ArticleUpdateUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Updates an article based on the given request.
   * Performs authorization, validation, patching, and updates the article in the database.
   *
   * @param request The update article request.
   * @return The updated article response.
   */
  def apply(request: UpdateArticleRequest): GetArticleResponse < Effect =
    database.transaction:
      for {
        article <- findArticle(request.slug)
        _       <- authorise(request, article)
        patches <- parse(request).validOrAbort
        updated <- patch(article, patches)
        updated <- generateNewSlug(article, updated)
        profile <- findProfile(request.requester)
        _       <- persistence.articles.update(article.data)
      } yield GetArticleResponse.make(article, profile, false, false)

  /**
   * Checks if the requester is authorized to update the article.
   *
   * @param request The update article request.
   * @param article The article to be updated.
   * @return Unit if authorized, otherwise fails with ArticleUpdateDenied.
   */
  private def authorise(request: UpdateArticleRequest, article: Article): Unit < Abort[Unauthorised] =
    if article.authorId == request.requester.userId then ()
    else Abort.fail(ArticleUpdateDenied)

  /**
   * Parses and validates the update request payload into a list of patches.
   *
   * @param request The update article request.
   * @return Validated list of patches to apply.
   */
  private def parse(request: UpdateArticleRequest): Validated[List[Patch]] < Any =
    Validation.validateAll:
      List.from(parseBody(request) ++ parseTitle(request) ++ parseDescription(request))

  /**
   * Parses and validates the body patch from the request.
   *
   * @param request The update article request.
   * @return Optional validated body patch.
   */
  private def parseBody(request: UpdateArticleRequest): Option[Validated[Patch]] =
    request.payload.article.body match
      case Some(body) if body.nonEmpty =>
        Some(ArticleInputValidation.body(body).map(Patch.Body(_)))
      case _                           => None

  /**
   * Parses and validates the title patch from the request.
   *
   * @param request The update article request.
   * @return Optional validated title patch.
   */
  private def parseTitle(request: UpdateArticleRequest): Option[Validated[Patch]] =
    request.payload.article.title match
      case Some(title) if title.nonEmpty =>
        Some(ArticleInputValidation.title(title).map(Patch.Title(_)))
      case _                             => None

  /**
   * Parses and validates the description patch from the request.
   *
   * @param request The update article request.
   * @return
   */
  private def parseDescription(request: UpdateArticleRequest): Option[Validated[Patch]] =
    request.payload.article.description match
      case Some(description) if description.nonEmpty =>
        Some(ArticleInputValidation.description(description).map(Patch.Description(_)))
      case _                                         => None

  /**
   * Applies the given patches to the article.
   *
   * @param article The original article.
   * @param patches List of patches to apply.
   * @return The patched article.
   */
  private def patch(article: Article, patches: List[Patch]): Article < Any =
    patches.foldLeft(article) {
      case article -> Patch.Body(body)               => article.copy(body = body)
      case article -> Patch.Title(title)             => article.copy(title = title)
      case article -> Patch.Description(description) => article.copy(description = description)
    }

  /**
   * Generates a new slug for the article if the title has changed.
   *
   * @param old The original article.
   * @param updated The updated article.
   * @return The article with a new slug if needed.
   */
  private def generateNewSlug(old: Article, updated: Article): Article < Sync =
    if old.title == updated.title then updated
    else IdGeneratorService.slug(updated.title).map(slug => updated.copy(slug = slug))

  /**
   * Finds the user profile for the given authenticated user.
   *
   * @param user The authenticated user.
   * @return The user's profile, or fails if missing.
   */
  private def findProfile(user: User.Authenticated): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUser(user.userId) ?! UserProfileMissing(user.userId)

  /**
   * Finds the article by its slug.
   *
   * @param slug The article slug.
   * @return The article, or fails if not found.
   */
  private def findArticle(slug: String): Article < (Effect & Env[Tx]) =
    persistence.articles.findBySlug(slug) ?! ArticleNotFound(slug)
}
