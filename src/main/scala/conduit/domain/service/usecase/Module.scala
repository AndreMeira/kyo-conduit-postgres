package conduit.domain.service.usecase

import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.service.validation.StateValidationService
import kyo.*

/**
 * Kyo Layer definitions for all use cases.
 *
 * Every layer is parameterised on the transaction type `Tx` so the domain
 * module stays independent of any concrete storage backend.
 *
 * Use cases fall into three dependency groups:
 *   - '''Basic''' — requires only `Database[Tx]` and `Persistence[Tx]`
 *   - '''Authenticated''' — additionally requires `AuthenticationService`
 *   - '''Full''' — additionally requires `StateValidationService[Tx]`
 */
object Module:

  /**
   * Provides the ArticleCreationUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for the creation of new articles in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleCreationUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleCreation[Tx <: Database.Transaction: Tag]: Layer[ArticleCreationUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ArticleCreationUseCase(database, persistence)
    }

  /**
   * Provides the ArticleDeletionUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for the deletion of existing articles from the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleDeletionUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleFavorite[Tx <: Database.Transaction: Tag]: Layer[ArticleFavoriteUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ArticleFavoriteUseCase(database, persistence)
    }

  /**
   * Provides the ArticleFeedUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving a feed of articles based on certain criteria.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleFeedUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleFeed[Tx <: Database.Transaction: Tag]: Layer[ArticleFeedUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ArticleFeedUseCase(database, persistence)
    }

  /**
   * Provides the ArticleUnfavoriteUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for removing an article from a user's list of favorites.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleUnfavoriteUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleUnfavorite[Tx <: Database.Transaction: Tag]: Layer[ArticleUnfavoriteUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ArticleUnfavoriteUseCase(database, persistence)
    }

  /**
   * Provides the ArticleUpdateUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for updating the details of an existing article in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleUpdateUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleUpdate[Tx <: Database.Transaction: Tag]: Layer[ArticleUpdateUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ArticleUpdateUseCase(database, persistence)
    }

  /**
   * Provides the ArticleDeletionUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for deleting an existing article from the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleDeletionUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleDeletion[Tx <: Database.Transaction: Tag]: Layer[ArticleDeletionUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ArticleDeletionUseCase(database, persistence)
    }

  /**
   * Provides the CommentAdditionUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for adding a new comment to an article in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides CommentAdditionUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def commentAddition[Tx <: Database.Transaction: Tag]: Layer[CommentAdditionUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      CommentAdditionUseCase(database, persistence)
    }

  /**
   * Provides the CommentDeletionUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for deleting an existing comment from an article in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides CommentDeletionUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def commentDeletion[Tx <: Database.Transaction: Tag]: Layer[CommentDeletionUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      CommentDeletionUseCase(database, persistence)
    }

  /**
   * Provides the ListArticlesUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving a list of articles based on certain criteria.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ListArticlesUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def listArticles[Tx <: Database.Transaction: Tag]: Layer[ListArticlesUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ListArticlesUseCase(database, persistence)
    }

  /**
   * Provides the ListCommentsUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving a list of comments for a specific article.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ListCommentsUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def listComments[Tx <: Database.Transaction: Tag]: Layer[ListCommentsUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ListCommentsUseCase(database, persistence)
    }

  /**
   * Provides the ListTagsUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving all available tags in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ListTagsUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def listTags[Tx <: Database.Transaction: Tag]: Layer[ListTagsUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ListTagsUseCase(database, persistence)
    }

  /**
   * Provides the ProfileFollowingUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for following another user's profile in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ProfileFollowingUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def profileFollowing[Tx <: Database.Transaction: Tag]: Layer[ProfileFollowingUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ProfileFollowingUseCase(database, persistence)
    }

  /**
   * Provides the ProfileReadUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving the profile information of a user in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ProfileReadUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def profileRead[Tx <: Database.Transaction: Tag]: Layer[ProfileReadUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ProfileReadUseCase(database, persistence)
    }

  /**
   * Provides the ProfileUnfollowingUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for unfollowing another user's profile in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ProfileUnfollowingUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def profileUnfollowing[Tx <: Database.Transaction: Tag]: Layer[ProfileUnfollowingUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx]) =>
      ProfileUnfollowingUseCase(database, persistence)
    }

  /**
   * Provides the UserReadUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving user information from the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides UserReadUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def userRead[Tx <: Database.Transaction: Tag]: Layer[UserReadUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]] & Env[AuthenticationService]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx], authentication: AuthenticationService) =>
      UserReadUseCase(database, persistence, authentication)
    }

  /**
   * Provides the ArticleReadUseCase layer, which depends on the Database, Persistence services, and AuthenticationService.
   * This use case allows for retrieving the details of a specific article,
   * and may require authentication to access certain information.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleReadUseCase[Tx] and requires Database[Tx], Persistence[Tx],
   *         and AuthenticationService in the environment.
   */
  def articleRead[Tx <: Database.Transaction: Tag]
    : Layer[ArticleReadUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]] & Env[AuthenticationService]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx], authentication: AuthenticationService) =>
      ArticleReadUseCase(database, persistence, authentication)
    }

  /**
   * Provides the UserAuthenticationUseCase layer,
   * which depends on the Database, Persistence services, and AuthenticationService.
   * This use case allows for authenticating a user based on their credentials
   * and may involve checking against stored user data.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides UserAuthenticationUseCase[Tx] and requires Database[Tx], Persistence[Tx],
   *         and AuthenticationService in the environment.
   */
  def userAuthentication[Tx <: Database.Transaction: Tag]
    : Layer[UserAuthenticationUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]] & Env[AuthenticationService]] =
    Layer.from { (database: Database[Tx], persistence: Persistence[Tx], authentication: AuthenticationService) =>
      UserAuthenticationUseCase(database, persistence, authentication)
    }

  /**
   * Provides the UserRegistrationUseCase layer, which depends on the Database,
   * Persistence services, AuthenticationService, and StateValidationService.
   * This use case allows for registering a new user in the system,
   * which may involve validating the input data and creating new records in the database.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides UserRegistrationUseCase[Tx] and requires Database[Tx], Persistence[Tx],
   *         AuthenticationService, and StateValidationService[Tx] in the environment.
   */
  def userRegistration[Tx <: Database.Transaction: Tag]
    : Layer[UserRegistrationUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]] & Env[AuthenticationService] & Env[StateValidationService[Tx]]] =
    Layer.from {
      (database: Database[Tx], persistence: Persistence[Tx], authentication: AuthenticationService, stateValidation: StateValidationService[Tx]) =>
        UserRegistrationUseCase(database, persistence, authentication, stateValidation)
    }

  /**
   * Provides the UserUpdateUseCase layer,
   * which depends on the Database, Persistence services, AuthenticationService, and StateValidationService.
   * This use case allows for updating an existing user's information in the system,
   * which may involve validating the new data and ensuring that the user is authenticated before making changes to their profile.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides UserUpdateUseCase[Tx] and requires Database[Tx], Persistence[Tx],
   *         AuthenticationService, and StateValidationService[Tx] in the environment.
   */
  def userUpdate[Tx <: Database.Transaction: Tag]
    : Layer[UserUpdateUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]] & Env[AuthenticationService] & Env[StateValidationService[Tx]]] =
    Layer.from {
      (database: Database[Tx], persistence: Persistence[Tx], authentication: AuthenticationService, stateValidation: StateValidationService[Tx]) =>
        UserUpdateUseCase(database, persistence, authentication, stateValidation)
    }

  // ---------------------------------------------------------------------------
  // Aggregate layer — produces a single UseCases[Tx] from all dependencies
  // ---------------------------------------------------------------------------

  /**
   * Provides a [[UseCases]] layer that aggregates all use cases into a single instance,
   * following the same pattern as [[conduit.domain.service.persistence.Persistence]]
   * for repositories.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides UseCases[Tx] and requires Database[Tx], Persistence[Tx],
   *         AuthenticationService, and StateValidationService[Tx] in the environment.
   */
  def useCases[Tx <: Database.Transaction: Tag]
    : Layer[UseCases[Tx], Env[Database[Tx]] & Env[Persistence[Tx]] & Env[AuthenticationService] & Env[StateValidationService[Tx]]] =
    Layer.from {
      (database: Database[Tx], persistence: Persistence[Tx], authentication: AuthenticationService, stateValidation: StateValidationService[Tx]) =>
        UseCases(
          userRegistration = UserRegistrationUseCase(database, persistence, authentication, stateValidation),
          userAuthentication = UserAuthenticationUseCase(database, persistence, authentication),
          userRead = UserReadUseCase(database, persistence, authentication),
          userUpdate = UserUpdateUseCase(database, persistence, authentication, stateValidation),
          profileRead = ProfileReadUseCase(database, persistence),
          profileFollowing = ProfileFollowingUseCase(database, persistence),
          profileUnfollowing = ProfileUnfollowingUseCase(database, persistence),
          articleCreation = ArticleCreationUseCase(database, persistence),
          articleRead = ArticleReadUseCase(database, persistence, authentication),
          articleUpdate = ArticleUpdateUseCase(database, persistence),
          articleFeed = ArticleFeedUseCase(database, persistence),
          listArticles = ListArticlesUseCase(database, persistence),
          articleFavorite = ArticleFavoriteUseCase(database, persistence),
          articleUnfavorite = ArticleUnfavoriteUseCase(database, persistence),
          articleDeletion = ArticleDeletionUseCase(database, persistence),
          commentAddition = CommentAdditionUseCase(database, persistence),
          commentDeletion = CommentDeletionUseCase(database, persistence),
          listComments = ListCommentsUseCase(database, persistence),
          listTags = ListTagsUseCase(database, persistence),
        )
    }
