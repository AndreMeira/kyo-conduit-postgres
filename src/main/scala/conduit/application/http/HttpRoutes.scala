package conduit.application.http

import conduit.application.http.errors.ErrorResponse
import conduit.application.http.auth.BearerToken
import conduit.domain.model.User
import conduit.domain.request.article.*
import conduit.domain.request.comment.*
import conduit.domain.request.user.*
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.usecase.UseCases
import conduit.infrastructure.codecs.http.JsonCodecs.given
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import kyo.*

/** HTTP route handlers for the Conduit API.
  *
  * Each handler wires a kyo-http route definition from [[Endpoint]] to a use
  * case from [[UseCases]], parsing the request body with Circe, handling
  * authentication, and mapping domain errors to JSON error responses.
  *
  * @param useCases  aggregated domain use cases
  * @param authentication  service for validating bearer tokens
  */
class HttpRoutes(useCases: UseCases[?], authentication: AuthenticationService) {

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  /** POST /api/users/login */
  val login = Endpoint.login.handler { req =>
    respond(HttpStatus.OK):
      parseBody[AuthenticateRequest.Payload](req.fields.body).map { payload =>
        val request = AuthenticateRequest(User.Anonymous, payload)
        useCases.userAuthentication(request).mapAbort(ErrorResponse.encode)
      }
  }

  /** POST /api/users */
  val register = Endpoint.register.handler { req =>
    respond(HttpStatus.Created):
      parseBody[RegistrationRequest.Payload](req.fields.body).map { payload =>
        val request = RegistrationRequest(User.Anonymous, payload)
        useCases.userRegistration(request).mapAbort(ErrorResponse.encode)
      }
  }

  // ---------------------------------------------------------------------------
  // User
  // ---------------------------------------------------------------------------

  /** GET /api/user */
  val getCurrentUser = Endpoint.getCurrentUser.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateRequired(req.fields.authorization)
        request   = GetUserRequest(user)
        response <- useCases.userRead(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** PUT /api/user */
  val updateUser = Endpoint.updateUser.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateRequired(req.fields.authorization)
        payload  <- parseBody[UpdateUserRequest.Payload](req.fields.body)
        request   = UpdateUserRequest(user, payload)
        response <- useCases.userUpdate(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  // ---------------------------------------------------------------------------
  // Profiles
  // ---------------------------------------------------------------------------

  /** GET /api/profiles/:username */
  val getProfile = Endpoint.getProfile.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateOptional(req.fields.authorization)
        request   = GetProfileRequest(user, req.fields.username)
        response <- useCases.profileRead(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** POST /api/profiles/:username/follow */
  val followUser = Endpoint.followUser.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateRequired(req.fields.authorization)
        request   = FollowUserRequest(user, req.fields.username)
        response <- useCases.profileFollowing(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** DELETE /api/profiles/:username/follow */
  val unfollowUser = Endpoint.unfollowUser.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateRequired(req.fields.authorization)
        request   = UnfollowUserRequest(user, req.fields.username)
        response <- useCases.profileUnfollowing(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  // ---------------------------------------------------------------------------
  // Articles
  // ---------------------------------------------------------------------------

  /** GET /api/articles */
  val listArticles = Endpoint.listArticles.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateOptional(req.fields.authorization)
        filters   = List(
                      req.fields.tag.map(ListArticlesRequest.Filter.Tag.apply),
                      req.fields.author.map(ListArticlesRequest.Filter.Author.apply),
                      req.fields.favorited.map(ListArticlesRequest.Filter.FavoriteOf.apply),
                    ).collect { case Present(filter) => filter }
        request   = ListArticlesRequest(user, req.fields.offset.getOrElse(0), req.fields.limit.getOrElse(20), filters)
        response <- useCases.listArticles(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** GET /api/articles/feed */
  val feedArticles = Endpoint.feedArticles.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateRequired(req.fields.authorization)
        request   = ArticleFeedRequest(user, req.fields.offset.getOrElse(0), req.fields.limit.getOrElse(20))
        response <- useCases.articleFeed(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** GET /api/articles/:slug */
  val getArticle = Endpoint.getArticle.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateOptional(req.fields.authorization)
        request   = GetArticleRequest(user, req.fields.slug)
        response <- useCases.articleRead(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** POST /api/articles */
  val createArticle = Endpoint.createArticle.handler { req =>
    respond(HttpStatus.Created):
      for
        user     <- authenticateRequired(req.fields.authorization)
        payload  <- parseBody[CreateArticleRequest.Payload](req.fields.body)
        request   = CreateArticleRequest(user, payload)
        response <- useCases.articleCreation(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** PUT /api/articles/:slug */
  val updateArticle = Endpoint.updateArticle.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateRequired(req.fields.authorization)
        payload  <- parseBody[UpdateArticleRequest.Payload](req.fields.body)
        request   = UpdateArticleRequest(user, req.fields.slug, payload)
        response <- useCases.articleUpdate(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** DELETE /api/articles/:slug */
  val deleteArticle = Endpoint.deleteArticle.handler { req =>
    respondEmpty(HttpStatus.NoContent):
      for
        user   <- authenticateRequired(req.fields.authorization)
        request = DeleteArticleRequest(user, req.fields.slug)
        _      <- useCases.articleDeletion(request).mapAbort(ErrorResponse.encode)
      yield ()
  }

  // ---------------------------------------------------------------------------
  // Favorites
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/favorite */
  val favoriteArticle = Endpoint.favoriteArticle.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateRequired(req.fields.authorization)
        request   = AddFavoriteArticleRequest(user, req.fields.slug)
        response <- useCases.articleFavorite(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** DELETE /api/articles/:slug/favorite */
  val unfavoriteArticle = Endpoint.unfavoriteArticle.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateRequired(req.fields.authorization)
        request   = RemoveFavoriteArticleRequest(user, req.fields.slug)
        response <- useCases.articleUnfavorite(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  // ---------------------------------------------------------------------------
  // Comments
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/comments */
  val addComment = Endpoint.addComment.handler { req =>
    respond(HttpStatus.Created):
      for
        user     <- authenticateRequired(req.fields.authorization)
        payload  <- parseBody[AddCommentRequest.Payload](req.fields.body)
        request   = AddCommentRequest(user, req.fields.slug, payload)
        response <- useCases.commentAddition(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** GET /api/articles/:slug/comments */
  val getComments = Endpoint.getComments.handler { req =>
    respond(HttpStatus.OK):
      for
        user     <- authenticateOptional(req.fields.authorization)
        request   = ListCommentsRequest(user, req.fields.slug)
        response <- useCases.listComments(request).mapAbort(ErrorResponse.encode)
      yield response
  }

  /** DELETE /api/articles/:slug/comments/:id */
  val deleteComment = Endpoint.deleteComment.handler { req =>
    respondEmpty(HttpStatus.NoContent):
      for
        user   <- authenticateRequired(req.fields.authorization)
        request = DeleteCommentRequest(user, req.fields.slug, req.fields.id)
        _      <- useCases.commentDeletion(request).mapAbort(ErrorResponse.encode)
      yield ()
  }

  // ---------------------------------------------------------------------------
  // Tags
  // ---------------------------------------------------------------------------

  /** GET /api/tags */
  val getTags = Endpoint.getTags.handler { _ =>
    respond(HttpStatus.OK) {
      val request = ListTagsRequest(User.Anonymous)
      useCases.listTags(request).mapAbort(ErrorResponse.encode)
    }
  }

  // ---------------------------------------------------------------------------
  // Collect all handlers
  // ---------------------------------------------------------------------------

  val all: Seq[HttpHandler[?, ?, ?]] = Seq(
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

  /**
   * Runs a domain program and renders it as a JSON HTTP response.
   * On success the value is Circe-encoded with the given status; on a domain
   * failure the [[ErrorResponse]] is rendered with its own status.
   */
  private def respond[A: Encoder](
    status: HttpStatus
  )(
    program: A < (Async & Abort[ErrorResponse])
  )(using Frame
  ): HttpResponse["body" ~ String] < Async = Abort.run[ErrorResponse](program).map {
    case Result.Success(value) => jsonResponse(status, value.asJson.noSpaces)
    case Result.Failure(error) => jsonResponse(error.status, errorJson(error))
    case Result.Panic(ex)      => throw ex
  }

  /**
   * Runs a domain program that produces no body. On success it renders an
   * empty response with the given status; failures render a JSON error.
   */
  private def respondEmpty(
    status: HttpStatus
  )(
    program: Unit < (Async & Abort[ErrorResponse])
  )(using Frame
  ): HttpResponse["body" ~ String] < Async = Abort.run[ErrorResponse](program).map {
    case Result.Success(_)     => HttpResponse(status).addField("body", "")
    case Result.Failure(error) => jsonResponse(error.status, errorJson(error))
    case Result.Panic(ex)      => throw ex
  }

  /** Builds a JSON HTTP response with the given status and body. */
  private def jsonResponse(status: HttpStatus, body: String): HttpResponse["body" ~ String] =
    HttpResponse(status).addField("body", body).addHeader("Content-Type", "application/json")

  /** Renders an [[ErrorResponse]] as the RealWorld `{ "errors": { ... } }` shape. */
  private def errorJson(error: ErrorResponse): String =
    Json.obj("errors" -> error.errors.asJson).noSpaces

  /** Parses a JSON request body, aborting with a 422 error when it is invalid. */
  private def parseBody[A: Decoder](raw: String): A < Abort[ErrorResponse] = Abort.get:
    decode[A](raw).left.map { _ =>
      ErrorResponse(HttpStatus.UnprocessableEntity, Map("body" -> List("is invalid")))
    }

  /**
   * Authenticates the user from the optional `Authorization` header.
   * Aborts with 401 when the token is missing or malformed.
   */
  private def authenticateRequired(header: Maybe[String]): User.Authenticated < (Async & Abort[ErrorResponse]) =
    BearerToken.fromHeader(header) match
      case Present(token) => authentication.authenticate(User.SignedToken(token)).mapAbort(ErrorResponse.encode)
      case Absent         => Abort.fail(ErrorResponse(HttpStatus.Unauthorized, Map("token" -> List("is missing"))))

  /**
   * Optionally authenticates the user from the `Authorization` header.
   * Returns `User.Anonymous` when no token is provided; aborts when the token
   * is present but invalid.
   */
  private def authenticateOptional(header: Maybe[String]): User < (Async & Abort[ErrorResponse]) =
    val maybeSignedToken = BearerToken.fromHeader(header).map(token => User.SignedToken(token))
    authentication.authenticate(maybeSignedToken).mapAbort(ErrorResponse.encode)
}
