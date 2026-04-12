package conduit.infrastructure.inmemory

import conduit.domain.error.ApplicationError
import conduit.domain.model.*
import conduit.domain.service.persistence.Persistence
import kyo.*

/**
 * Test support for [[InMemoryDatabase]] based repository specs.
 *
 * Mirrors [[conduit.infrastructure.postgres.PostgresTestSupport]] but against
 * the in-memory implementation. Every call to [[WithCleanDatabase]] builds a
 * brand-new empty [[InMemoryState]] and wires a fresh [[Persistence]] so tests
 * are fully isolated from each other without needing Flyway, containers or
 * any external resource.
 */
object InMemoryTestSupport:

  /** 
   * Type alias for the effect stack used in tests. Every test runs with Async,
   * Sync and Scope to manage resources, plus Abort for error handling and
   * Env[InMemoryTransaction] to run effects inside a transaction context.
   */
  def withDatabase[A, Effect](
    testBody: InMemoryDatabase => A < (Effect & Scope & Sync)
  ): A < (Effect & Scope & Sync) =
    for
      state       <- emptyState
      database     = new InMemoryDatabase(state)
      persistence <- makePersistence
      result      <- testBody(database)
    yield result

  /**
   * Builds a freshly initialised, empty [[InMemoryState]]. Each atomic
   * reference is allocated with its zero value so the resulting state contains
   * no articles, users, followers, etc.
   */
  def emptyState: InMemoryState < Sync =
    for
      lock        <- Meter.initMutexUnscoped
      articles    <- AtomicRef.init(Map.empty[Article.Id, Article])
      comments    <- AtomicRef.init(Map.empty[Comment.Id, Comment])
      profiles    <- AtomicRef.init(Map.empty[UserProfile.Id, UserProfile])
      favorites   <- AtomicRef.init(Map.empty[User.Id, List[Article.Id]])
      followers   <- AtomicRef.init(Map.empty[User.Id, List[UserProfile.Id]])
      credentials <- AtomicRef.init(Map.empty[User.Id, Credentials.Hashed])
      tags        <- AtomicRef.init(Map.empty[Article.Id, List[String]])
      changes     <- AtomicRef.init(List.empty[InMemoryState.Changed])
    yield new InMemoryState(
      lock,
      articles,
      comments,
      profiles,
      favorites,
      followers,
      credentials,
      tags,
      changes,
    )

  // ---------------------------------------------------------------------------
  // Repository wiring
  // ---------------------------------------------------------------------------

  /**
   * Builds a [[Persistence]] populated with all the in-memory repositories.
   * The comment repository needs a dedicated [[Meter]] to serialise id
   * generation; a fresh mutex is allocated for each persistence instance.
   */
  def makePersistence: Persistence[InMemoryTransaction] < Sync =
    Meter.initMutexUnscoped.map { commentLock =>
      Persistence(
        articles = InMemoryArticleRepository(),
        users = InMemoryUserProfileRepository(),
        followers = InMemoryFollowerRepository(),
        favorites = InMemoryFavoriteRepository(),
        credentials = InMemoryCredentialsRepository(),
        comments = InMemoryCommentRepository(commentLock),
        tags = InMemoryTagRepository(),
      )
    }
