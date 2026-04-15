package conduit.application.http

import conduit.application.http.errors.ErrorResponse
import conduit.application.http.types.*
import conduit.domain.request.article.{ CreateArticleRequest, UpdateArticleRequest }
import conduit.domain.request.comment.AddCommentRequest
import conduit.domain.request.user.{ AuthenticateRequest, RegistrationRequest, UpdateUserRequest }
import conduit.domain.response.article.{ ArticleListResponse, GetArticleResponse, TagListResponse }
import conduit.domain.response.comment.{ CommentListResponse, GetCommentResponse }
import conduit.domain.response.user.{ AuthenticationResponse, GetProfileResponse }
import conduit.infrastructure.codecs.http.JsonCodecs.given
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.{ EndpointInput, * }
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
        .and(jsonBody[(errors: Map[String, List[String]])])
        .map((status, body) => ErrorResponse(status, body.errors)): error =>
          (error.status, (errors = error.errors))
    )

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  /** POST /api/users/login — Authenticate an existing user. */
  val login: Endpoint[Unit, AuthenticateRequest.Payload, ErrorResponse, AuthenticationResponse, Any] =
    api.post
      .in("users" / "login")
      .in(jsonBody[AuthenticateRequest.Payload])
      .out(jsonBody[AuthenticationResponse])

  /** POST /api/users — Register a new user. */
  val register: Endpoint[Unit, RegistrationRequest.Payload, ErrorResponse, AuthenticationResponse, Any] =
    api.post
      .in("users")
      .in(jsonBody[RegistrationRequest.Payload])
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[AuthenticationResponse])

  // ---------------------------------------------------------------------------
  // User
  // ---------------------------------------------------------------------------

  /** GET /api/user — Get the currently authenticated user. */
  val getCurrentUser: Endpoint[Option[BearerToken], Unit, ErrorResponse, AuthenticationResponse, Any] =
    api.get
      .securityIn(authenticated)
      .in("user")
      .out(jsonBody[AuthenticationResponse])

  /** PUT /api/user — Update the currently authenticated user. */
  val updateUser: Endpoint[Option[BearerToken], UpdateUserRequest.Payload, ErrorResponse, AuthenticationResponse, Any] =
    api.put
      .securityIn(authenticated)
      .in("user")
      .in(jsonBody[UpdateUserRequest.Payload])
      .out(jsonBody[AuthenticationResponse])

  // ---------------------------------------------------------------------------
  // Profiles
  // ---------------------------------------------------------------------------

  /** GET /api/profiles/:username — Get a user profile. Auth optional. */
  val getProfile: Endpoint[Option[BearerToken], UserName, ErrorResponse, GetProfileResponse, Any] =
    api.get
      .securityIn(anyone)
      .in("profiles" / path[String]("username"))
      .out(jsonBody[GetProfileResponse])

  /** POST /api/profiles/:username/follow — Follow a user. */
  val followUser: Endpoint[Option[BearerToken], UserName, ErrorResponse, GetProfileResponse, Any] =
    api.post
      .securityIn(authenticated)
      .in("profiles" / path[String]("username") / "follow")
      .out(jsonBody[GetProfileResponse])

  /** DELETE /api/profiles/:username/follow — Unfollow a user. */
  val unfollowUser: Endpoint[Option[BearerToken], UserName, ErrorResponse, GetProfileResponse, Any] =
    api.delete
      .securityIn(authenticated)
      .in("profiles" / path[String]("username") / "follow")
      .out(jsonBody[GetProfileResponse])

  // ---------------------------------------------------------------------------
  // Articles
  // ---------------------------------------------------------------------------

  /** GET /api/articles — List articles with optional filters. Auth optional. */
  val listArticles
    : Endpoint[Option[BearerToken], (Option[String], Option[String], Option[String], Option[Int], Option[Int]), ErrorResponse, ArticleListResponse, Any] =
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
  val feedArticles: Endpoint[Option[BearerToken], (Option[Int], Option[Int]), ErrorResponse, ArticleListResponse, Any] =
    api.get
      .securityIn(authenticated)
      .in("articles" / "feed")
      .in(query[Option[Int]]("offset"))
      .in(query[Option[Int]]("limit"))
      .out(jsonBody[ArticleListResponse])

  /** GET /api/articles/:slug — Get a single article. Auth optional. */
  val getArticle: Endpoint[Option[BearerToken], Slug, ErrorResponse, GetArticleResponse, Any] =
    api.get
      .securityIn(anyone)
      .in("articles" / path[String]("slug"))
      .out(jsonBody[GetArticleResponse])

  /** POST /api/articles — Create an article. Auth required. */
  val createArticle: Endpoint[Option[BearerToken], CreateArticleRequest.Payload, ErrorResponse, GetArticleResponse, Any] =
    api.post
      .securityIn(authenticated)
      .in("articles")
      .in(jsonBody[CreateArticleRequest.Payload])
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[GetArticleResponse])

  /** PUT /api/articles/:slug — Update an article. Auth required. */
  val updateArticle: Endpoint[Option[BearerToken], (Slug, UpdateArticleRequest.Payload), ErrorResponse, GetArticleResponse, Any] =
    api.put
      .securityIn(authenticated)
      .in("articles" / path[String]("slug"))
      .in(jsonBody[UpdateArticleRequest.Payload])
      .out(jsonBody[GetArticleResponse])

  /** DELETE /api/articles/:slug — Delete an article. Auth required. */
  val deleteArticle: Endpoint[Option[BearerToken], Slug, ErrorResponse, Unit, Any] =
    api.delete
      .securityIn(authenticated)
      .in("articles" / path[String]("slug"))
      .out(statusCode(StatusCode.NoContent))

  // ---------------------------------------------------------------------------
  // Favorites
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/favorite — Favorite an article. Auth required. */
  val favoriteArticle: Endpoint[Option[BearerToken], Slug, ErrorResponse, GetArticleResponse, Any] =
    api.post
      .securityIn(authenticated)
      .in("articles" / path[String]("slug") / "favorite")
      .out(jsonBody[GetArticleResponse])

  /** DELETE /api/articles/:slug/favorite — Unfavorite an article. Auth required. */
  val unfavoriteArticle: Endpoint[Option[BearerToken], Slug, ErrorResponse, GetArticleResponse, Any] =
    api.delete
      .securityIn(authenticated)
      .in("articles" / path[String]("slug") / "favorite")
      .out(jsonBody[GetArticleResponse])

  // ---------------------------------------------------------------------------
  // Comments
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/comments — Add a comment to an article. Auth required. */
  val addComment: Endpoint[Option[BearerToken], (Slug, AddCommentRequest.Payload), ErrorResponse, GetCommentResponse, Any] =
    api.post
      .securityIn(authenticated)
      .in("articles" / path[String]("slug") / "comments")
      .in(jsonBody[AddCommentRequest.Payload])
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[GetCommentResponse])

  /** GET /api/articles/:slug/comments — Get comments for an article. Auth optional. */
  val getComments: Endpoint[Option[BearerToken], Slug, ErrorResponse, CommentListResponse, Any] =
    api.get
      .securityIn(anyone)
      .in("articles" / path[String]("slug") / "comments")
      .out(jsonBody[CommentListResponse])

  /** DELETE /api/articles/:slug/comments/:id — Delete a comment. Auth required. */
  val deleteComment: Endpoint[Option[BearerToken], (String, Long), ErrorResponse, Unit, Any] =
    api.delete
      .securityIn(authenticated)
      .in("articles" / path[String]("slug") / "comments" / path[Long]("id"))
      .out(statusCode(StatusCode.NoContent))

  // ---------------------------------------------------------------------------
  // Tags
  // ---------------------------------------------------------------------------

  /** GET /api/tags — Get all tags. */
  val getTags: Endpoint[Unit, Unit, ErrorResponse, TagListResponse, Any] =
    api.get
      .in("tags")
      .out(jsonBody[TagListResponse])

  /**
   * Required authentication — extracts the token if present.
   * Returns `None` when the header is absent.
   * Returns a decode failure when the header is present but does not use the `Token` scheme.
   * The handler is responsible for failing with 401 when `None`.
   */
  def authenticated: EndpointInput.Auth[Option[BearerToken], EndpointInput.AuthType.ApiKey] =
    auth.apiKey(header[Option[BearerToken]]("Authorization"))

  /** Optional authentication — same extraction, same semantics. */
  def anyone: EndpointInput.Auth[Option[BearerToken], EndpointInput.AuthType.ApiKey] =
    auth.apiKey(header[Option[BearerToken]]("Authorization"))
