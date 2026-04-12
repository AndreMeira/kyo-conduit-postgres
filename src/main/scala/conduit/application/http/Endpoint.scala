package conduit.application.http

import conduit.application.http.types.{BearerToken, NamedSearch, Search}
import conduit.domain.request.article.{CreateArticleRequest, UpdateArticleRequest}
import conduit.domain.request.comment.AddCommentRequest
import conduit.domain.request.user.{AuthenticateRequest, RegistrationRequest, UpdateUserRequest}
import conduit.domain.response.article.{ArticleListResponse, GetArticleResponse, TagListResponse}
import conduit.domain.response.comment.{CommentListResponse, GetCommentResponse}
import conduit.domain.response.user.{AuthenticationResponse, GetProfileResponse}
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
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
  // Error response — matches the Conduit spec format:
  // {"errors": {"body": ["can't be empty", "is too short"]}}
  // ---------------------------------------------------------------------------

  case class ErrorBody(errors: Map[String, List[String]])

  // ---------------------------------------------------------------------------
  // Base endpoints
  // ---------------------------------------------------------------------------

  /** Public base — no authentication by default. */
  private val api = endpoint
    .in("api")
    .errorOut(statusCode.and(jsonBody[ErrorBody]))

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  /** POST /api/users/login — Authenticate an existing user. */
  val login: Endpoint[Unit, AuthenticateRequest.Payload, (StatusCode, ErrorBody), AuthenticationResponse, Any] =
    api.post
      .in("users" / "login")
      .in(jsonBody[AuthenticateRequest.Payload])
      .out(jsonBody[AuthenticationResponse])

  /** POST /api/users — Register a new user. */
  val register: Endpoint[Unit, RegistrationRequest.Payload, (StatusCode, ErrorBody), AuthenticationResponse, Any] =
    api.post
      .in("users")
      .in(jsonBody[RegistrationRequest.Payload])
      .out(jsonBody[AuthenticationResponse])

  // ---------------------------------------------------------------------------
  // User
  // ---------------------------------------------------------------------------

  /** GET /api/user — Get the currently authenticated user. */
  val getCurrentUser: Endpoint[BearerToken, Unit, (StatusCode, ErrorBody), AuthenticationResponse, Any] =
    api.get
      .securityIn(auth.bearer[BearerToken]())
      .in("user")
      .out(jsonBody[AuthenticationResponse])

  /** PUT /api/user — Update the currently authenticated user. */
  val updateUser: Endpoint[BearerToken, UpdateUserRequest.Payload, (StatusCode, ErrorBody), AuthenticationResponse, Any] =
    api.put
      .securityIn(auth.bearer[BearerToken]())
      .in("user")
      .in(jsonBody[UpdateUserRequest.Payload])
      .out(jsonBody[AuthenticationResponse])

  // ---------------------------------------------------------------------------
  // Profiles
  // ---------------------------------------------------------------------------

  /** GET /api/profiles/:username — Get a user profile. Auth optional. */
  val getProfile: Endpoint[Option[BearerToken], String, (StatusCode, ErrorBody), GetProfileResponse, Any] =
    api.get
      .securityIn(auth.bearer[Option[BearerToken]]())
      .in("profiles" / path[String]("username"))
      .out(jsonBody[GetProfileResponse])

  /** POST /api/profiles/:username/follow — Follow a user. */
  val followUser: Endpoint[BearerToken, String, (StatusCode, ErrorBody), GetProfileResponse, Any] =
    api.post
      .securityIn(auth.bearer[BearerToken]())
      .in("profiles" / path[String]("username") / "follow")
      .out(jsonBody[GetProfileResponse])

  /** DELETE /api/profiles/:username/follow — Unfollow a user. */
  val unfollowUser: Endpoint[BearerToken, String, (StatusCode, ErrorBody), GetProfileResponse, Any] =
    api.delete
      .securityIn(auth.bearer[BearerToken]())
      .in("profiles" / path[String]("username") / "follow")
      .out(jsonBody[GetProfileResponse])

  // ---------------------------------------------------------------------------
  // Articles
  // ---------------------------------------------------------------------------

  /** GET /api/articles — List articles with optional filters. Auth optional. */
  val listArticles
    : Endpoint[Option[BearerToken], Search, (StatusCode, ErrorBody), ArticleListResponse, Any] =
    api.get
      .securityIn(auth.bearer[Option[BearerToken]]())
      .in("articles")
      .in(query[Option[String]]("tag"))
      .in(query[Option[String]]("author"))
      .in(query[Option[String]]("favorited"))
      .in(query[Option[Int]]("limit"))
      .in(query[Option[Int]]("offset"))
      .out(jsonBody[ArticleListResponse])

  /** GET /api/articles/feed — Get articles from followed users. Auth required. */
  val feedArticles: Endpoint[BearerToken, (Option[Int], Option[Int]), (StatusCode, ErrorBody), ArticleListResponse, Any] =
    api.get
      .securityIn(auth.bearer[BearerToken]())
      .in("articles" / "feed")
      .in(query[Option[Int]]("limit"))
      .in(query[Option[Int]]("offset"))
      .out(jsonBody[ArticleListResponse])

  /** GET /api/articles/:slug — Get a single article. Auth optional. */
  val getArticle: Endpoint[Option[BearerToken], String, (StatusCode, ErrorBody), GetArticleResponse, Any] =
    api.get
      .securityIn(auth.bearer[Option[BearerToken]]())
      .in("articles" / path[String]("slug"))
      .out(jsonBody[GetArticleResponse])

  /** POST /api/articles — Create an article. Auth required. */
  val createArticle: Endpoint[BearerToken, CreateArticleRequest.Payload, (StatusCode, ErrorBody), GetArticleResponse, Any] =
    api.post
      .securityIn(auth.bearer[BearerToken]())
      .in("articles")
      .in(jsonBody[CreateArticleRequest.Payload])
      .out(jsonBody[GetArticleResponse])

  /** PUT /api/articles/:slug — Update an article. Auth required. */
  val updateArticle: Endpoint[BearerToken, (String, UpdateArticleRequest.Payload), (StatusCode, ErrorBody), GetArticleResponse, Any] =
    api.put
      .securityIn(auth.bearer[BearerToken]())
      .in("articles" / path[String]("slug"))
      .in(jsonBody[UpdateArticleRequest.Payload])
      .out(jsonBody[GetArticleResponse])

  /** DELETE /api/articles/:slug — Delete an article. Auth required. */
  val deleteArticle: Endpoint[BearerToken, String, (StatusCode, ErrorBody), Unit, Any] =
    api.delete
      .securityIn(auth.bearer[BearerToken]())
      .in("articles" / path[String]("slug"))

  // ---------------------------------------------------------------------------
  // Favorites
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/favorite — Favorite an article. Auth required. */
  val favoriteArticle: Endpoint[BearerToken, String, (StatusCode, ErrorBody), GetArticleResponse, Any] =
    api.post
      .securityIn(auth.bearer[BearerToken]())
      .in("articles" / path[String]("slug") / "favorite")
      .out(jsonBody[GetArticleResponse])

  /** DELETE /api/articles/:slug/favorite — Unfavorite an article. Auth required. */
  val unfavoriteArticle: Endpoint[BearerToken, String, (StatusCode, ErrorBody), GetArticleResponse, Any] =
    api.delete
      .securityIn(auth.bearer[BearerToken]())
      .in("articles" / path[String]("slug") / "favorite")
      .out(jsonBody[GetArticleResponse])

  // ---------------------------------------------------------------------------
  // Comments
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/comments — Add a comment to an article. Auth required. */
  val addComment: Endpoint[BearerToken, (String, AddCommentRequest.Payload), (StatusCode, ErrorBody), GetCommentResponse, Any] =
    api.post
      .securityIn(auth.bearer[BearerToken]())
      .in("articles" / path[String]("slug") / "comments")
      .in(jsonBody[AddCommentRequest.Payload])
      .out(jsonBody[GetCommentResponse])

  /** GET /api/articles/:slug/comments — Get comments for an article. Auth optional. */
  val getComments: Endpoint[Option[BearerToken], String, (StatusCode, ErrorBody), CommentListResponse, Any] =
    api.get
      .securityIn(auth.bearer[Option[BearerToken]]())
      .in("articles" / path[String]("slug") / "comments")
      .out(jsonBody[CommentListResponse])

  /** DELETE /api/articles/:slug/comments/:id — Delete a comment. Auth required. */
  val deleteComment: Endpoint[BearerToken, (String, Long), (StatusCode, ErrorBody), Unit, Any] =
    api.delete
      .securityIn(auth.bearer[BearerToken]())
      .in("articles" / path[String]("slug") / "comments" / path[Long]("id"))

  // ---------------------------------------------------------------------------
  // Tags
  // ---------------------------------------------------------------------------

  /** GET /api/tags — Get all tags. */
  val getTags: Endpoint[Unit, Unit, (StatusCode, ErrorBody), TagListResponse, Any] =
    api.get
      .in("tags")
      .out(jsonBody[TagListResponse])
