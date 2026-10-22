package conduit.infrastructure.postgres

import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.infrastructure.postgres.configuration.{ DatabaseConfig, DatasourceConfig, MigrationConfig }
import kyo.*

/**
 * Kyo Layer definitions for the kyo-sql Postgres infrastructure.
 *
 * Provides layers for the [[PostgresDatabase]], the [[Persistence]] backed by the kyo-sql
 * repositories, the [[PostgresClient]] connection pool, and the [[Migration]] of the schema.
 */
object Module:

  /**
   * The complete layer that provides both Persistence and Database services, along with
   * the migration of the schema. This is the main entry point for integrating the Postgres
   * infrastructure into the application.
   */
  lazy val all: Layer[
    Database[PostgresTransaction] & Persistence[PostgresTransaction] & Migration,
    Env[DatabaseConfig] & Async & Scope & Abort[PostgresDatabase.Error | Migration.Error],
  ] =
    datasourceConfig
      .using(client and migrationConfig)
      .to(database and persistence and migration)

  /**
   * Extracts the DatasourceConfig from the loaded DatabaseConfig. This allows
   * the client layer to depend only on the specific configuration it needs,
   * rather than the entire DatabaseConfig.
   */
  lazy val datasourceConfig: Layer[DatasourceConfig, Env[DatabaseConfig] & Sync] =
    Layer.from((dbConfig: DatabaseConfig) => dbConfig.datasource)

  /**
   * Extracts the MigrationConfig from the loaded DatabaseConfig. This allows
   * the migration layer to depend only on the specific configuration it needs,
   * rather than the entire DatabaseConfig.
   */
  lazy val migrationConfig: Layer[MigrationConfig, Env[DatabaseConfig] & Sync] =
    Layer.from((dbConfig: DatabaseConfig) => dbConfig.migration)

  /**
   * Provides the pooled PostgresClient described by the DatasourceConfig, closed when
   * the enclosing Scope ends.
   */
  lazy val client: Layer[PostgresClient, Env[DatasourceConfig] & Async & Scope & Abort[PostgresDatabase.Error]] =
    Layer.from((config: DatasourceConfig) => PostgresDatabase.client(config))

  /**
   * Provides a PostgresDatabase instance that implements the Database trait using
   * the PostgresClient. This layer depends on the client and provides the
   * database service to the application.
   */
  lazy val database: Layer[Database[PostgresTransaction], Env[PostgresClient]] =
    Layer.from: (client: PostgresClient) =>
      PostgresDatabase(client): Database[PostgresTransaction]

  /**
   * Provides a [[Migration]] instance configured with Flyway, using the
   * [[MigrationConfig]] for Flyway settings and the [[DatasourceConfig]] for the
   * database it connects to over JDBC.
   */
  lazy val migration: Layer[Migration, Env[MigrationConfig] & Env[DatasourceConfig] & Sync & Abort[Migration.Error]] =
    Layer {
      for
        config     <- Env.get[MigrationConfig]
        datasource <- Env.get[DatasourceConfig]
        flyway     <- Kyo
                        .attempt(config.toFlywayConfig.dataSource(datasource.jdbcUrl, datasource.user, datasource.password).load())
                        .mapAbort(Migration.Error.MigrationFailed(_))
      yield Migration(flyway)
    }

  /**
   * Provides a Persistence instance that implements the Persistence trait using
   * the kyo-sql Postgres repositories.
   */
  lazy val persistence: Layer[Persistence[PostgresTransaction], Any] =
    Layer {
      Persistence(
        articles = PostgresArticleRepository(),
        users = PostgresUserProfileRepository(),
        followers = PostgresFollowerRepository(),
        favorites = PostgresFavoriteRepository(),
        credentials = PostgresCredentialsRepository(),
        comments = PostgresCommentRepository(),
        tags = PostgresTagRepository(),
      )
    }
