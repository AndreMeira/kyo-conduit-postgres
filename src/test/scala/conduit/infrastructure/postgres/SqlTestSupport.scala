package conduit.infrastructure.postgres

import conduit.domain.error.ApplicationError
import conduit.domain.service.persistence.Persistence
import kyo.*
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Test support for the kyo-sql Postgres repositories: a [[PostgresDatabase]] over a live
 * PostgreSQL container, started once per suite and stopped when the Scope closes.
 *
 * {{{
 *   SqlTestSupport.withDatabase { database =>
 *     "MyRepository" should {
 *       "save and find" in
 *         database.withMigration:
 *           database.transaction:
 *             for ... yield assert(...)
 *     }
 *   }
 * }}}
 */
object SqlTestSupport:

  private val migrationLocation = "classpath:migrations/schema"

  /**
   * The kyo-sql repositories, wired into one [[Persistence]].
   *
   * @return the persistence layer
   */
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
   * Provides `withMigration`, which runs a test body against a freshly migrated schema.
   *
   * @param flyway the migrations of the suite's database
   */
  class WithDatabaseExtension(flyway: Flyway):
    extension (database: PostgresDatabase)

      /**
       * Runs the body against a freshly migrated schema, cleaned once the body ends.
       *
       * @param body the test body
       * @tparam A the result of the body
       * @tparam Effect the effects of the body
       * @return the result of the body
       */
      def withMigration[A, Effect <: Async & Abort[ApplicationError]](body: => A < Effect): A < Effect =
        Scope.run:
          Scope.acquireRelease(flyway.migrate())(_ => flyway.clean()) *> body

  /**
   * Provides a [[PostgresDatabase]] over a live PostgreSQL container for the duration of the
   * surrounding Scope. The schema is migrated per test, by `withMigration`.
   *
   * @param test the suite body, given the database
   * @tparam A the result of the suite body
   * @return the result of the suite body
   */
  def withDatabase[A](test: WithDatabaseExtension ?=> PostgresDatabase => A < (Async & Scope)): A < (Async & Scope) =
    PostgresTestSupport.withContainer { container =>
      PostgresClient.init(PostgresTestSupport.connectionUrl(container)).orPanic.map { client =>
        test(using WithDatabaseExtension(flyway(container)))(PostgresDatabase(client))
      }
    }

  /**
   * Builds the Flyway instance migrating the container's database, with clean enabled so
   * tests can reset the schema. Migrations are scanned through the test classpath's classloader.
   *
   * @param container the running container
   * @return the Flyway instance
   */
  private def flyway(container: PostgreSQLContainer[?]): Flyway =
    Flyway
      .configure(getClass.getClassLoader)
      .dataSource(container.getJdbcUrl, container.getUsername, container.getPassword)
      .locations(migrationLocation)
      .cleanDisabled(false)
      .loggers("slf4j")
      .load()
