package conduit.application.http

import conduit.application.http.errors.ErrorResponse
import conduit.application.http.types.BearerToken
import conduit.domain.model.User
import conduit.domain.request.article.*
import conduit.domain.request.comment.*
import conduit.domain.request.user.*
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.usecase.UseCases
import kyo.*
import sttp.model.StatusCodes

/** HTTP route handlers for the Conduit API.
  *
  * Each route wires a Tapir endpoint definition from [[Endpoint]] to a use case
  * from [[UseCases]], handling authentication and error mapping.
  *
  * @param useCases  aggregated domain use cases
  * @param authentication  service for validating bearer tokens
  */
class HttpRoutes(useCases: UseCases[?], authentication: AuthenticationService) extends StatusCodes {

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  /** POST /api/users/login */
  val login: Unit < Routes = Routes.add(Endpoint.login) { payload =>
    val request = AuthenticateRequest(User.Anonymous, payload)
    useCases.userAuthentication(request).mapAbort(ErrorResponse.encode)
  }

  /** POST /api/users */
  val register: Unit < Routes = Routes.add(Endpoint.register) { payload =>
    val request = RegistrationRequest(User.Anonymous, payload)
    useCases.userRegistration(request).mapAbort(ErrorResponse.encode)
  }

  // ---------------------------------------------------------------------------
  // User
  // ---------------------------------------------------------------------------

  /** GET /api/user */
  val getCurrentUser: Unit < Routes = Routes.add(Endpoint.getCurrentUser) { token =>
    for
      user     <- authenticateRequired(token)
      request   = GetUserRequest(user)
      response <- useCases.userRead(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** PUT /api/user */
  val updateUser: Unit < Routes = Routes.add(Endpoint.updateUser) { (token, payload) =>
    for
      user     <- authenticateRequired(token)
      request   = UpdateUserRequest(user, payload)
      response <- useCases.userUpdate(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  // ---------------------------------------------------------------------------
  // Profiles
  // ---------------------------------------------------------------------------

  /** GET /api/profiles/:username */
  val getProfile: Unit < Routes = Routes.add(Endpoint.getProfile) { (maybeToken, username) =>
    for
      user     <- authenticateOptional(maybeToken)
      request   = GetProfileRequest(user, username)
      response <- useCases.profileRead(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** POST /api/profiles/:username/follow */
  val followUser: Unit < Routes = Routes.add(Endpoint.followUser) { (token, username) =>
    for
      user     <- authenticateRequired(token)
      request   = FollowUserRequest(user, username)
      response <- useCases.profileFollowing(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** DELETE /api/profiles/:username/follow */
  val unfollowUser: Unit < Routes = Routes.add(Endpoint.unfollowUser) { (token, username) =>
    for
      user     <- authenticateRequired(token)
      request   = UnfollowUserRequest(user, username)
      response <- useCases.profileUnfollowing(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  // ---------------------------------------------------------------------------
  // Articles
  // ---------------------------------------------------------------------------

  /** GET /api/articles */
  val listArticles: Unit < Routes = Routes.add(Endpoint.listArticles) { (maybeToken, tag, author, favorited, offset, limit) =>
    for
      user     <- authenticateOptional(maybeToken)
      filters   = List(
                    tag.map(ListArticlesRequest.Filter.Tag.apply),
                    author.map(ListArticlesRequest.Filter.Author.apply),
                    favorited.map(ListArticlesRequest.Filter.FavoriteOf.apply),
                  ).flatten
      request   = ListArticlesRequest(user, offset.getOrElse(0), limit.getOrElse(20), filters)
      response <- useCases.listArticles(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** GET /api/articles/feed */
  val feedArticles: Unit < Routes = Routes.add(Endpoint.feedArticles) { (token, offset, limit) =>
    for
      user     <- authenticateRequired(token)
      request   = ArticleFeedRequest(user, offset.getOrElse(0), limit.getOrElse(20))
      response <- useCases.articleFeed(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** GET /api/articles/:slug */
  val getArticle: Unit < Routes = Routes.add(Endpoint.getArticle) { (maybeToken, slug) =>
    for
      user     <- authenticateOptional(maybeToken)
      request   = GetArticleRequest(user, slug)
      response <- useCases.articleRead(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** POST /api/articles */
  val createArticle: Unit < Routes = Routes.add(Endpoint.createArticle) { (token, payload) =>
    for
      user     <- authenticateRequired(token)
      request   = CreateArticleRequest(user, payload)
      response <- useCases.articleCreation(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** PUT /api/articles/:slug */
  val updateArticle: Unit < Routes = Routes.add(Endpoint.updateArticle) { (token, slug, payload) =>
    for
      user     <- authenticateRequired(token)
      request   = UpdateArticleRequest(user, slug, payload)
      response <- useCases.articleUpdate(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** DELETE /api/articles/:slug */
  val deleteArticle: Unit < Routes = Routes.add(Endpoint.deleteArticle) { (token, slug) =>
    for
      user   <- authenticateRequired(token)
      request = DeleteArticleRequest(user, slug)
      _      <- useCases.articleDeletion(request).mapAbort(ErrorResponse.encode)
    yield ()
  }

  // ---------------------------------------------------------------------------
  // Favorites
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/favorite */
  val favoriteArticle: Unit < Routes = Routes.add(Endpoint.favoriteArticle) { (token, slug) =>
    for
      user     <- authenticateRequired(token)
      request   = AddFavoriteArticleRequest(user, slug)
      response <- useCases.articleFavorite(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** DELETE /api/articles/:slug/favorite */
  val unfavoriteArticle: Unit < Routes = Routes.add(Endpoint.unfavoriteArticle) { (token, slug) =>
    for
      user     <- authenticateRequired(token)
      request   = RemoveFavoriteArticleRequest(user, slug)
      response <- useCases.articleUnfavorite(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  // ---------------------------------------------------------------------------
  // Comments
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/comments */
  val addComment: Unit < Routes = Routes.add(Endpoint.addComment) { (token, slug, payload) =>
    for
      user     <- authenticateRequired(token)
      request   = AddCommentRequest(user, slug, payload)
      response <- useCases.commentAddition(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** GET /api/articles/:slug/comments */
  val getComments: Unit < Routes = Routes.add(Endpoint.getComments) { (maybeToken, slug) =>
    for
      user     <- authenticateOptional(maybeToken)
      request   = ListCommentsRequest(user, slug)
      response <- useCases.listComments(request).mapAbort(ErrorResponse.encode)
    yield response
  }

  /** DELETE /api/articles/:slug/comments/:id */
  val deleteComment: Unit < Routes = Routes.add(Endpoint.deleteComment) { (token, slug, commentId) =>
    for
      user   <- authenticateRequired(token)
      request = DeleteCommentRequest(user, slug, commentId)
      _      <- useCases.commentDeletion(request).mapAbort(ErrorResponse.encode)
    yield ()
  }

  // ---------------------------------------------------------------------------
  // Tags
  // ---------------------------------------------------------------------------

  /** GET /api/tags */
  val getTags: Unit < Routes = Routes.add(Endpoint.getTags) { _ =>
    val request = ListTagsRequest(User.Anonymous)
    useCases.listTags(request).mapAbort(ErrorResponse.encode)
  }

  // ---------------------------------------------------------------------------
  // Collect all routes
  // ---------------------------------------------------------------------------

  val all: Unit < Routes = Routes.collect(
    login,
    register,
    getCurrentUser,
    updateUser,
    getProfile,
    followUser,
    unfollowUser,
    listArticles,
    feedArticles,
    getArticle,
    createArticle,
    updateArticle,
    deleteArticle,
    favoriteArticle,
    unfavoriteArticle,
    addComment,
    getComments,
    deleteComment,
    getTags,
  )

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private def authenticateRequired(maybeToken: Option[BearerToken]): User.Authenticated < (Async & Abort[ErrorResponse]) =
    maybeToken match
      case None        => Abort.fail(ErrorResponse(Unauthorized, Map("token" -> List("is missing"))))
      case Some(token) => authentication.authenticate(User.SignedToken(token)).mapAbort(ErrorResponse.encode)

  private def authenticateOptional(maybeToken: Option[BearerToken]): User < (Async & Abort[ErrorResponse]) =
    val maybeSignedToken = Maybe.fromOption(maybeToken.map(token => User.SignedToken(token)))
    authentication.authenticate(maybeSignedToken).mapAbort(ErrorResponse.encode)
}
