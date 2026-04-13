package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.request.article.CreateArticleRequest
import conduit.domain.response.article.GetArticleResponse
import conduit.domain.service.persistence.{ Database, IdGeneratorService, Persistence }
import conduit.domain.service.validation.ArticleInputValidation
import conduit.domain.syntax.*
import kyo.*
import zio.prelude.Validation

/**
 * Use case for creating a new article.
 *
 * This use case handles the creation of articles by authenticated users.
 * It validates the article input, generates necessary identifiers (UUID and slug),
 * saves the article within a database transaction, and returns the created article
 * with author profile information.
 *
 * @param database The database abstraction for managing transactions.
 * @param persistence The persistence layer providing access to repositories.
 * @tparam Tx The type of database transaction.
 */
class ArticleCreationUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Creates a new article based on the authenticated request.
   *
   * Executes within a database transaction to:
   * 1. Parse and validate the article data
   * 2. Find the author's profile
   * 3. Save the article to the repository
   * 4. Return the article response with favorited and following flags set to false
   *
   * @param request The request containing the requester and article data.
   * @return The created article response wrapped in Effect context.
   */
  def apply(request: CreateArticleRequest): GetArticleResponse < Effect =
    database.transaction:
      for {
        article <- parse(request).validOrAbort
        profile <- findProfile(request.requester)
        _       <- persistence.articles.save(article)
        _       <- persistence.tags.add(article.id, article.tags)
      } yield GetArticleResponse.make(article, profile, favorited = false, following = false)

  /**
   * Parses and validates the article creation request.
   *
   * Generates a unique ID and slug for the article, validates the body, title,
   * description, and tags, and combines them into a validated Article instance.
   *
   * @param request The article creation request containing the payload.
   * @return A validated Article or validation errors wrapped in Effect context.
   */
  def parse(request: CreateArticleRequest): Validated[Article] < Effect =
    for {
      now  <- Clock.now.map(_.toJava)
      id   <- IdGeneratorService.uuid
      slug <- IdGeneratorService.slug(request.payload.article.title)
    } yield Validation.validateWith(
      ArticleInputValidation.body(request.payload.article.body),
      ArticleInputValidation.title(request.payload.article.title),
      ArticleInputValidation.description(request.payload.article.description),
      ArticleInputValidation.tags(request.payload.article.tagList.getOrElse(List.empty)),
    ) { (body, title, description, tags) =>
      Article(id, slug, title, description, body, request.requester.userId, favoriteCount = 0, tags, now, now)
    }

  /**
   * Finds the user profile for the authenticated user.
   *
   * @param user The authenticated user whose profile should be retrieved.
   * @return The user profile if found, or aborts with UserProfileMissing error.
   */
  def findProfile(user: User.Authenticated): UserProfile < (Effect & Env[Tx]) =
    persistence.users.findByUser(user.userId) ?! UserProfileMissing(user.userId)
}
