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
  def articleCreation[Tx <: Database.Transaction: Tag]
    : Layer[ArticleCreationUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ArticleCreationUseCase(db, p)
    }

  /**
   * Provides the ArticleDeletionUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for the deletion of existing articles from the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleDeletionUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleFavorite[Tx <: Database.Transaction: Tag]
    : Layer[ArticleFavoriteUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ArticleFavoriteUseCase(db, p)
    }

  /**
   * Provides the ArticleFeedUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving a feed of articles based on certain criteria.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleFeedUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleFeed[Tx <: Database.Transaction: Tag]
    : Layer[ArticleFeedUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ArticleFeedUseCase(db, p)
    }

  /**
   * Provides the ArticleUnfavoriteUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for removing an article from a user's list of favorites.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleUnfavoriteUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleUnfavorite[Tx <: Database.Transaction: Tag]
    : Layer[ArticleUnfavoriteUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ArticleUnfavoriteUseCase(db, p)
    }

  /**
   * Provides the ArticleUpdateUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for updating the details of an existing article in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ArticleUpdateUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def articleUpdate[Tx <: Database.Transaction: Tag]
    : Layer[ArticleUpdateUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ArticleUpdateUseCase(db, p)
    }

  /**
   * Provides the CommentAdditionUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for adding a new comment to an article in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides CommentAdditionUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def commentAddition[Tx <: Database.Transaction: Tag]
    : Layer[CommentAdditionUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      CommentAdditionUseCase(db, p)
    }

  /**
   * Provides the CommentDeletionUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for deleting an existing comment from an article in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides CommentDeletionUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def commentDeletion[Tx <: Database.Transaction: Tag]
    : Layer[CommentDeletionUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      CommentDeletionUseCase(db, p)
    }

  /**
   * Provides the ListArticlesUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving a list of articles based on certain criteria.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ListArticlesUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def listArticles[Tx <: Database.Transaction: Tag]
    : Layer[ListArticlesUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ListArticlesUseCase(db, p)
    }

  /**
   * Provides the ListCommentsUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving a list of comments for a specific article.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ListCommentsUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def listComments[Tx <: Database.Transaction: Tag]
    : Layer[ListCommentsUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ListCommentsUseCase(db, p)
    }

  /**
   * Provides the ProfileFollowingUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for following another user's profile in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ProfileFollowingUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def profileFollowing[Tx <: Database.Transaction: Tag]
    : Layer[ProfileFollowingUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ProfileFollowingUseCase(db, p)
    }

  /**
   * Provides the ProfileReadUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving the profile information of a user in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ProfileReadUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def profileRead[Tx <: Database.Transaction: Tag]
    : Layer[ProfileReadUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ProfileReadUseCase(db, p)
    }

  /**
   * Provides the ProfileUnfollowingUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for unfollowing another user's profile in the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides ProfileUnfollowingUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def profileUnfollowing[Tx <: Database.Transaction: Tag]
    : Layer[ProfileUnfollowingUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      ProfileUnfollowingUseCase(db, p)
    }

  /**
   * Provides the UserReadUseCase layer, which depends on the Database and Persistence services.
   * This use case allows for retrieving user information from the system.
   *
   * @tparam Tx The transaction type, which must be a subtype of Database.Transaction and have a Tag instance.
   * @return A Layer that provides UserReadUseCase[Tx] and requires Database[Tx] and Persistence[Tx] in the environment.
   */
  def userRead[Tx <: Database.Transaction: Tag]
    : Layer[UserReadUseCase[Tx], Env[Database[Tx]] & Env[Persistence[Tx]]] =
    Layer.from { (db: Database[Tx], p: Persistence[Tx]) =>
      UserReadUseCase(db, p)
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
    Layer.from { (db: Database[Tx], p: Persistence[Tx], auth: AuthenticationService) =>
      ArticleReadUseCase(db, p, auth)
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
    Layer.from { (db: Database[Tx], p: Persistence[Tx], auth: AuthenticationService) =>
      UserAuthenticationUseCase(db, p, auth)
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
    Layer.from { (db: Database[Tx], p: Persistence[Tx], auth: AuthenticationService, sv: StateValidationService[Tx]) =>
      UserRegistrationUseCase(db, p, auth, sv)
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
    Layer.from { (db: Database[Tx], p: Persistence[Tx], auth: AuthenticationService, sv: StateValidationService[Tx]) =>
      UserUpdateUseCase(db, p, auth, sv)
    }
