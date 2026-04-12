package conduit.infrastructure.inmemory

import conduit.domain.service.persistence.{ Database, Persistence }
import kyo.*

/**
 * Kyo Layer definitions for the in-memory infrastructure.
 *
 * Provides layers for the [[InMemoryDatabase]] and [[Persistence]] backed
 * by in-memory repositories. Useful for testing without external dependencies.
 */
object Module:

  /**
   * The complete layer that provides both Persistence and Database services, along with
   * the necessary in-memory state. This is the main entry point for integrating the
   * in-memory infrastructure into the application.
   */
  lazy val all: Layer[
    Persistence[InMemoryTransaction] & Database[InMemoryTransaction],
    Sync & Scope,
  ] = Layer.init[Persistence[InMemoryTransaction] & Database[InMemoryTransaction]](
    state,
    database,
    persistence,
  )

  /**
   * Layer that provides the in-memory state. This is a simple mutable state that
   * holds all the data for the in-memory repositories. It is initialized as empty
   * and can be shared across the database and persistence layers.
   */
  lazy val state: Layer[InMemoryState, Sync & Scope] = Layer {
    InMemoryState.empty
  }

  /**
   * Provides an InMemoryDatabase instance that implements the Database trait using
   * the provided in-memory state. This layer depends on the state and provides the
   * database functionality for transactions.
   */
  lazy val database: Layer[Database[InMemoryTransaction], Env[InMemoryState]] =
    Layer.from { (state: InMemoryState) =>
      InMemoryDatabase(state): Database[InMemoryTransaction]
    }

  /**
   * Provides a Persistence instance that implements the Persistence trait using
   * in-memory repositories. This layer initializes all the repositories with the
   * shared in-memory state and a mutex for comment operations to ensure thread safety.
   * The persistence layer provides the main interface for data access and manipulation
   * in the application.
   */
  lazy val persistence: Layer[Persistence[InMemoryTransaction], Sync] =
    Layer {
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
    }
