package conduit.application.http

import conduit.application.http.types.*
import conduit.domain.request.article.{CreateArticleRequest, UpdateArticleRequest}
import conduit.domain.request.comment.AddCommentRequest
import conduit.domain.request.user.{AuthenticateRequest, RegistrationRequest, UpdateUserRequest}
import conduit.domain.response.article.{ArticleListResponse, GetArticleResponse, TagListResponse}
import conduit.domain.response.comment.{CommentListResponse, GetCommentResponse}
import conduit.domain.response.user.{AuthenticationResponse, GetProfileResponse}
import conduit.infrastructure.codecs.http.ErrorBody
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.EndpointInput.AuthType
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

/** Tapir endpoint definitions for the Conduit API.
  *
  * Each val describes the HTTP shape of an endpoint (method, path, query params, security, request/response bodies) without
  * any handler logic. Handlers are wired separately via [[kyo.Routes.add]].
  *
  * Endpoints are grouped by resource:
  *   - Authentication (login, register)
  *   - User (get current, update)
  *   - Profiles (get, follow, unfollow)
  *   - Articles (list, feed, get, create, update, delete)
  *   - Favorites (favorite, unfavorite)
  *   - Comments (add, list, delete)
  *   - Tags (list)
  */
object Endpoint:

  // ---------------------------------------------------------------------------
  // Base endpoints
  // ---------------------------------------------------------------------------

  /** Public base — no authentication by default. */
  private val api = endpoint
    .in("api")
    .errorOut(
      statusCode
        .and(jsonBody[Map[String, List[String]]])
        .map((status, errors) => ErrorBody(status, errors))(error => (error.status, error.errors))
    )

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  /** POST /api/users/login — Authenticate an existing user. */
  val login: Endpoint[Unit, AuthenticateRequest.Payload, ErrorBody, AuthenticationResponse, Any] =
    api.post
      .in("users" / "login")
      .in(jsonBody[AuthenticateRequest.Payload])
      .out(jsonBody[AuthenticationResponse])

  /** POST /api/users — Register a new user. */
  val register: Endpoint[Unit, RegistrationRequest.Payload, ErrorBody, AuthenticationResponse, Any] =
    api.post
      .in("users")
      .in(jsonBody[RegistrationRequest.Payload])
      .out(jsonBody[AuthenticationResponse])

  // ---------------------------------------------------------------------------
  // User
  // ---------------------------------------------------------------------------

  /** GET /api/user — Get the currently authenticated user. */
  val getCurrentUser: Endpoint[BearerToken, Unit, ErrorBody, AuthenticationResponse, Any] =
    api.get
      .securityIn(authenticated)
      .in("user")
      .out(jsonBody[AuthenticationResponse])

  /** PUT /api/user — Update the currently authenticated user. */
  val updateUser: Endpoint[BearerToken, UpdateUserRequest.Payload, ErrorBody, AuthenticationResponse, Any] =
    api.put
      .securityIn(authenticated)
      .in("user")
      .in(jsonBody[UpdateUserRequest.Payload])
      .out(jsonBody[AuthenticationResponse])

  // ---------------------------------------------------------------------------
  // Profiles
  // ---------------------------------------------------------------------------

  /** GET /api/profiles/:username — Get a user profile. Auth optional. */
  val getProfile: Endpoint[Option[BearerToken], UserName, ErrorBody, GetProfileResponse, Any] =
    api.get
      .securityIn(anyone)
      .in("profiles" / path[String]("username"))
      .out(jsonBody[GetProfileResponse])

  /** POST /api/profiles/:username/follow — Follow a user. */
  val followUser: Endpoint[BearerToken, UserName, ErrorBody, GetProfileResponse, Any] =
    api.post
      .securityIn(authenticated)
      .in("profiles" / path[String]("username") / "follow")
      .out(jsonBody[GetProfileResponse])

  /** DELETE /api/profiles/:username/follow — Unfollow a user. */
  val unfollowUser: Endpoint[BearerToken, UserName, ErrorBody, GetProfileResponse, Any] =
    api.delete
      .securityIn(authenticated)
      .in("profiles" / path[String]("username") / "follow")
      .out(jsonBody[GetProfileResponse])

  // ---------------------------------------------------------------------------
  // Articles
  // ---------------------------------------------------------------------------

  /** GET /api/articles — List articles with optional filters. Auth optional. */
  val listArticles: Endpoint[Option[BearerToken], Search, ErrorBody, ArticleListResponse, Any] =
    api.get
      .securityIn(anyone)
      .in("articles")
      .in(query[Option[String]]("tag"))
      .in(query[Option[String]]("author"))
      .in(query[Option[String]]("favorited"))
      .in(query[Option[Int]]("offset"))
      .in(query[Option[Int]]("limit"))
      .out(jsonBody[ArticleListResponse])

  /** GET /api/articles/feed — Get articles from followed users. Auth required. */
  val feedArticles: Endpoint[BearerToken, Page, ErrorBody, ArticleListResponse, Any] =
    api.get
      .securityIn(authenticated)
      .in("articles" / "feed")
      .in(query[Option[Int]]("offset"))
      .in(query[Option[Int]]("limit"))
      .out(jsonBody[ArticleListResponse])

  /** GET /api/articles/:slug — Get a single article. Auth optional. */
  val getArticle: Endpoint[Option[BearerToken], Slug, ErrorBody, GetArticleResponse, Any] =
    api.get
      .securityIn(anyone)
      .in("articles" / path[String]("slug"))
      .out(jsonBody[GetArticleResponse])

  /** POST /api/articles — Create an article. Auth required. */
  val createArticle: Endpoint[BearerToken, CreateArticleRequest.Payload, ErrorBody, GetArticleResponse, Any] =
    api.post
      .securityIn(authenticated)
      .in("articles")
      .in(jsonBody[CreateArticleRequest.Payload])
      .out(jsonBody[GetArticleResponse])

  /** PUT /api/articles/:slug — Update an article. Auth required. */
  val updateArticle: Endpoint[BearerToken, (Slug, UpdateArticleRequest.Payload), ErrorBody, GetArticleResponse, Any] =
    api.put
      .securityIn(authenticated)
      .in("articles" / path[String]("slug"))
      .in(jsonBody[UpdateArticleRequest.Payload])
      .out(jsonBody[GetArticleResponse])

  /** DELETE /api/articles/:slug — Delete an article. Auth required. */
  val deleteArticle: Endpoint[BearerToken, Slug, ErrorBody, Unit, Any] =
    api.delete
      .securityIn(authenticated)
      .in("articles" / path[String]("slug"))

  // ---------------------------------------------------------------------------
  // Favorites
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/favorite — Favorite an article. Auth required. */
  val favoriteArticle: Endpoint[BearerToken, Slug, ErrorBody, GetArticleResponse, Any] =
    api.post
      .securityIn(authenticated)
      .in("articles" / path[String]("slug") / "favorite")
      .out(jsonBody[GetArticleResponse])

  /** DELETE /api/articles/:slug/favorite — Unfavorite an article. Auth required. */
  val unfavoriteArticle: Endpoint[BearerToken, Slug, ErrorBody, GetArticleResponse, Any] =
    api.delete
      .securityIn(authenticated)
      .in("articles" / path[String]("slug") / "favorite")
      .out(jsonBody[GetArticleResponse])

  // ---------------------------------------------------------------------------
  // Comments
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/comments — Add a comment to an article. Auth required. */
  val addComment: Endpoint[BearerToken, (Slug, AddCommentRequest.Payload), ErrorBody, GetCommentResponse, Any] =
    api.post
      .securityIn(authenticated)
      .in("articles" / path[String]("slug") / "comments")
      .in(jsonBody[AddCommentRequest.Payload])
      .out(jsonBody[GetCommentResponse])

  /** GET /api/articles/:slug/comments — Get comments for an article. Auth optional. */
  val getComments: Endpoint[Option[BearerToken], Slug, ErrorBody, CommentListResponse, Any] =
    api.get
      .securityIn(anyone)
      .in("articles" / path[String]("slug") / "comments")
      .out(jsonBody[CommentListResponse])

  /** DELETE /api/articles/:slug/comments/:id — Delete a comment. Auth required. */
  val deleteComment: Endpoint[BearerToken, (String, Long), ErrorBody, Unit, Any] =
    api.delete
      .securityIn(authenticated)
      .in("articles" / path[String]("slug") / "comments" / path[Long]("id"))

  // ---------------------------------------------------------------------------
  // Tags
  // ---------------------------------------------------------------------------

  /** GET /api/tags — Get all tags. */
  val getTags: Endpoint[Unit, Unit, ErrorBody, TagListResponse, Any] =
    api.get
      .in("tags")
      .out(jsonBody[TagListResponse])

  /**
   * Helper for authenticated endpoints that want to reuse the same logic for optional vs required auth. For example, the
   * GET /api/articles endpoint can be accessed by both authenticated and unauthenticated users, but the handler logic is mostly
   * the same except for some personalization based on the user. This helper allows us to define a single endpoint with optional
   * auth and then handle the presence or absence of the token in the handler logic.
   */
  def authenticated: EndpointInput.Auth[BearerToken, AuthType.Http] =
    auth.bearer[BearerToken]()

  /**
   * Helper for authenticated endpoints that require a token.
   * This is just a shorthand for the more verbose syntax used in the
   * endpoint definitions, but it can be useful for consistency and readability.
   */
  def anyone: EndpointInput.Auth[Option[BearerToken], AuthType.Http] =
    auth.bearer[Option[BearerToken]]()
