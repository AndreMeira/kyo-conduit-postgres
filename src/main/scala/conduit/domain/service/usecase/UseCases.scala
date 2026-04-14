package conduit.domain.service.usecase

import conduit.domain.service.persistence.Database.Transaction

/**
 * Aggregates all use cases in the application, following the same pattern as
 * [[conduit.domain.service.persistence.Persistence]] for repositories.
 *
 * This class provides a single access point to all use cases, making it easy
 * to pass them as a dependency to the HTTP layer or any other consumer.
 *
 * @tparam Tx the transaction type used for database operations
 *
 * @param userRegistration    registers a new user
 * @param userAuthentication  authenticates an existing user
 * @param userRead            retrieves the current user
 * @param userUpdate          updates the current user
 * @param profileRead         retrieves a user profile
 * @param profileFollowing    follows a user profile
 * @param profileUnfollowing  unfollows a user profile
 * @param articleCreation     creates a new article
 * @param articleRead         retrieves a single article
 * @param articleUpdate       updates an existing article
 * @param articleFeed         retrieves articles from followed users
 * @param listArticles        lists articles with filters
 * @param articleFavorite     favorites an article
 * @param articleUnfavorite   unfavorites an article
 * @param articleDeletion     deletes an article
 * @param commentAddition     adds a comment to an article
 * @param commentDeletion     deletes a comment
 * @param listComments        lists comments for an article
 * @param listTags            lists all available tags
 */
class UseCases[Tx <: Transaction](
  val userRegistration: UserRegistrationUseCase[Tx],
  val userAuthentication: UserAuthenticationUseCase[Tx],
  val userRead: UserReadUseCase[Tx],
  val userUpdate: UserUpdateUseCase[Tx],
  val profileRead: ProfileReadUseCase[Tx],
  val profileFollowing: ProfileFollowingUseCase[Tx],
  val profileUnfollowing: ProfileUnfollowingUseCase[Tx],
  val articleCreation: ArticleCreationUseCase[Tx],
  val articleRead: ArticleReadUseCase[Tx],
  val articleUpdate: ArticleUpdateUseCase[Tx],
  val articleFeed: ArticleFeedUseCase[Tx],
  val listArticles: ListArticlesUseCase[Tx],
  val articleFavorite: ArticleFavoriteUseCase[Tx],
  val articleUnfavorite: ArticleUnfavoriteUseCase[Tx],
  val articleDeletion: ArticleDeletionUseCase[Tx],
  val commentAddition: CommentAdditionUseCase[Tx],
  val commentDeletion: CommentDeletionUseCase[Tx],
  val listComments: ListCommentsUseCase[Tx],
  val listTags: ListTagsUseCase[Tx],
)
