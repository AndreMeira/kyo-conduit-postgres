package conduit.infrastructure.postgres

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import conduit.domain.error.ApplicationError
import conduit.domain.service.persistence.Persistence
import kyo.*
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Test support for Postgres repository integration tests.
 *
 * Manages the full lifecycle of a PostgreSQL Testcontainer within a Kyo Scope:
 * the container is started and a fully wired (runTest, Persistence) pair is
 * provided to the test body. Everything is torn down automatically when the
 * Scope closes.
 *
 * Each individual test is executed through the provided `runTest` runner,
 * which resets the database (Flyway clean + migrate) before the test body
 * runs and then executes the body inside a transaction. This guarantees
 * every test starts from an identical, empty, freshly-migrated schema —
 * no cross-test data leakage.
 *
 * Typical usage inside a KyoTestSuite.specSuite:
 * {{{
 *   PostgresTestSupport.withDatabase { (runTest, persistence) =>
 *     "MyRepository" should {
 *       "save and find" in {
 *         runTest:
 *           for
 *             _     <- persistence.users.save(...)
 *             found <- persistence.users.find(...)
 *           yield assert(found == ...)
 *       }
 *     }
 *   }
 * }}}
 */
object PostgresTestSupport:

  private val postgresImageName = "postgres:17"
  private val migrationLocation = "classpath:migrations/schema"

  // ---------------------------------------------------------------------------
  // Container lifecycle
  // ---------------------------------------------------------------------------

  private def startContainer: PostgreSQLContainer[?] < Sync =
    Kyo.defer:
      val container = PostgreSQLContainer(postgresImageName)
      container.start()
      container

  private def stopContainer(container: PostgreSQLContainer[?]): Unit < Sync =
    Kyo.defer(container.stop())

  // ---------------------------------------------------------------------------
  // DataSource lifecycle
  // ---------------------------------------------------------------------------

  private def createDatasource(container: PostgreSQLContainer[?]): HikariDataSource < Sync =
    Kyo.defer:
      val config = HikariConfig()
      config.setJdbcUrl(container.getJdbcUrl)
      config.setUsername(container.getUsername)
      config.setPassword(container.getPassword)
      config.setMaximumPoolSize(5)
      config.setAutoCommit(false)
      HikariDataSource(config)

  private def closeDatasource(datasource: HikariDataSource): Unit < Sync =
    Kyo.defer(datasource.close())

  // ---------------------------------------------------------------------------
  // Migrations
  // ---------------------------------------------------------------------------

  /**
   * Builds a Flyway instance configured against the given datasource, with
   * clean enabled so tests can reset the schema between runs.
   */
  private def flyway(datasource: HikariDataSource): Flyway =
    Flyway
      .configure()
      .dataSource(datasource)
      .locations(migrationLocation)
      .cleanDisabled(false)
      .loggers("slf4j")
      .load()

  // ---------------------------------------------------------------------------
  // Repository wiring
  // ---------------------------------------------------------------------------

  def makePersistence: Persistence[PostgresTransaction] =
    Persistence(
      articles = PostgresArticleRepository(),
      users = PostgresUserProfileRepository(),
      followers = PostgresFollowerRepository(),
      favorites = PostgresFavoriteRepository(),
      credentials = PostgresCredentialsRepository(),
      comments = PostgresCommentRepository(),
      tags = PostgresTagRepository(),
    )

  /**
   * Provides a `withMigration` method that runs a test body with a clean database.
   *
   * The body is executed inside a transaction, so it can call any repository
   * method directly — just as the Postgres spec bodies do.
   */
  trait WithDatabaseExtension {
    extension (database: PostgresDatabase) {

      /** 
       * Runs the given body with a clean database. The body is executed inside a
       * transaction, so it can call any repository method directly — just as
       * the Postgres spec bodies do.
       *
       * @param body the test body to execute with a clean database
       * @return the result of the test body
       */
      def withMigration[A, Effect <: Async & Abort[ApplicationError]](body: => A < Effect): A < Effect =
        Scope.run:
          for
            migration <- Kyo.lift(flyway(database.datasource))
            _         <- Scope.acquireRelease(migration.migrate())(_ => migration.clean())
            result    <- body
          yield result
    }
  }

  // ---------------------------------------------------------------------------
  // Public fixture
  // ---------------------------------------------------------------------------

  /**
   * Provides a (WithDatabase, Persistence) pair for the duration of the surrounding
   * Scope, backed by a live PostgreSQL container.
   *
   * The container is started once per spec suite; the schema is reset before
   * every test body is executed via `runTest`.
   *
   * @param test the test body that receives the per-test runner and the
   *             persistence layer
   * @return the result of the test body
   */
  def withDatabase[A](
    test: WithDatabaseExtension ?=> PostgresDatabase => A < (Async & Scope)
  ): A < (Async & Scope) =
    for
      container  <- Scope.acquireRelease(startContainer)(stopContainer)
      datasource <- Scope.acquireRelease(createDatasource(container))(closeDatasource)
      database    = PostgresDatabase(datasource)
      result     <- test(using new WithDatabaseExtension {})(database)
    yield result
