package conduit.application.http

import conduit.domain.types.{ ArticleSlug, ProfileName, TagName }
import conduit.infrastructure.codecs.http.JsonCodecs.given
import kyo.HttpRoute
import kyo.HttpPath.*

import scala.language.implicitConversions

/** kyo-http route definitions for the Conduit API.
  *
  * Each val describes the HTTP shape of an endpoint (method, path, captures,
  * query params, optional authentication header, request/response bodies)
  * without any handler logic. Handlers are wired separately in [[HttpRoutes]].
  *
  * Bodies are declared as plain text (`bodyText`): request bodies are parsed
  * and response bodies are produced as JSON strings by Circe in the handlers,
  * keeping JSON serialisation independent of kyo-http's own `Schema` codecs.
  *
  * Optional authentication is modelled as an optional `Authorization` header
  * (`headerOpt[String]`). The handler parses the `Token <jwt>` scheme and
  * decides whether authentication is required.
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

  /** Optional `Authorization` header carrying the `Token <jwt>` credential. */
  private def withAuth[In](req: HttpRoute.RequestDef[In]) =
    req.headerOpt[String]("authorization", wireName = "Authorization")

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  /** POST /api/users/login — Authenticate an existing user. */
  val login =
    HttpRoute.postRaw("api" / "users" / "login").request(_.bodyText).response(_.bodyText)

  /** POST /api/users — Register a new user. */
  val register =
    HttpRoute.postRaw("api" / "users").request(_.bodyText).response(_.bodyText)

  // ---------------------------------------------------------------------------
  // User
  // ---------------------------------------------------------------------------

  /** GET /api/user — Get the currently authenticated user. */
  val getCurrentUser =
    HttpRoute.getRaw("api" / "user").request(withAuth).response(_.bodyText)

  /** PUT /api/user — Update the currently authenticated user. */
  val updateUser =
    HttpRoute.putRaw("api" / "user").request(withAuth(_).bodyText).response(_.bodyText)

  // ---------------------------------------------------------------------------
  // Profiles
  // ---------------------------------------------------------------------------

  /** GET /api/profiles/:username — Get a user profile. Auth optional. */
  val getProfile =
    HttpRoute.getRaw("api" / "profiles" / Capture[ProfileName]("username")).request(withAuth).response(_.bodyText)

  /** POST /api/profiles/:username/follow — Follow a user. */
  val followUser =
    HttpRoute.postRaw("api" / "profiles" / Capture[ProfileName]("username") / "follow").request(withAuth).response(_.bodyText)

  /** DELETE /api/profiles/:username/follow — Unfollow a user. */
  val unfollowUser =
    HttpRoute.deleteRaw("api" / "profiles" / Capture[ProfileName]("username") / "follow").request(withAuth).response(_.bodyText)

  // ---------------------------------------------------------------------------
  // Articles
  // ---------------------------------------------------------------------------

  /** GET /api/articles — List articles with optional filters. Auth optional. */
  val listArticles =
    HttpRoute.getRaw("api" / "articles")
      .request(
        withAuth(_)
          .queryOpt[TagName]("tag")
          .queryOpt[ProfileName]("author")
          .queryOpt[ProfileName]("favorited")
          .queryOpt[Int]("offset")
          .queryOpt[Int]("limit")
      )
      .response(_.bodyText)

  /** GET /api/articles/feed — Get articles from followed users. Auth required. */
  val feedArticles =
    HttpRoute.getRaw("api" / "articles" / "feed")
      .request(withAuth(_).queryOpt[Int]("offset").queryOpt[Int]("limit"))
      .response(_.bodyText)

  /** GET /api/articles/:slug — Get a single article. Auth optional. */
  val getArticle =
    HttpRoute.getRaw("api" / "articles" / Capture[ArticleSlug]("slug")).request(withAuth).response(_.bodyText)

  /** POST /api/articles — Create an article. Auth required. */
  val createArticle =
    HttpRoute.postRaw("api" / "articles").request(withAuth(_).bodyText).response(_.bodyText)

  /** PUT /api/articles/:slug — Update an article. Auth required. */
  val updateArticle =
    HttpRoute.putRaw("api" / "articles" / Capture[ArticleSlug]("slug")).request(withAuth(_).bodyText).response(_.bodyText)

  /** DELETE /api/articles/:slug — Delete an article. Auth required. */
  val deleteArticle =
    HttpRoute.deleteRaw("api" / "articles" / Capture[ArticleSlug]("slug")).request(withAuth).response(_.bodyText)

  // ---------------------------------------------------------------------------
  // Favorites
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/favorite — Favorite an article. Auth required. */
  val favoriteArticle =
    HttpRoute.postRaw("api" / "articles" / Capture[ArticleSlug]("slug") / "favorite").request(withAuth).response(_.bodyText)

  /** DELETE /api/articles/:slug/favorite — Unfavorite an article. Auth required. */
  val unfavoriteArticle =
    HttpRoute.deleteRaw("api" / "articles" / Capture[ArticleSlug]("slug") / "favorite").request(withAuth).response(_.bodyText)

  // ---------------------------------------------------------------------------
  // Comments
  // ---------------------------------------------------------------------------

  /** POST /api/articles/:slug/comments — Add a comment to an article. Auth required. */
  val addComment =
    HttpRoute.postRaw("api" / "articles" / Capture[ArticleSlug]("slug") / "comments").request(withAuth(_).bodyText).response(_.bodyText)

  /** GET /api/articles/:slug/comments — Get comments for an article. Auth optional. */
  val getComments =
    HttpRoute.getRaw("api" / "articles" / Capture[ArticleSlug]("slug") / "comments").request(withAuth).response(_.bodyText)

  /** DELETE /api/articles/:slug/comments/:id — Delete a comment. Auth required. */
  val deleteComment =
    HttpRoute.deleteRaw("api" / "articles" / Capture[ArticleSlug]("slug") / "comments" / Capture[Long]("id")).request(withAuth).response(_.bodyText)

  // ---------------------------------------------------------------------------
  // Tags
  // ---------------------------------------------------------------------------

  /** GET /api/tags — Get all tags. */
  val getTags =
    HttpRoute.getRaw("api" / "tags").response(_.bodyText)
