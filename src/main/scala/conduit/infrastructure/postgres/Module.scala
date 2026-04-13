package conduit.infrastructure.postgres

import com.zaxxer.hikari.HikariDataSource
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.infrastructure.configuration.ConfigurationLoader
import conduit.infrastructure.postgres.configuration.{ DatabaseConfig, DatasourceConfig, MigrationConfig }
import kyo.*
import org.flywaydb.core.Flyway

/**
 * Kyo Layer definitions for the Postgres infrastructure.
 *
 * Provides layers for the [[PostgresDatabase]], [[Persistence]] backed by
 * Postgres repositories, and the [[HikariDataSource]] connection pool.
 */
object Module:

  /**
   * The complete layer that provides both Persistence and Database services, along with
   * the necessary configuration and resources. This is the main entry point for
   * integrating the Postgres infrastructure into the application.
   */
  lazy val all: Layer[Database[PostgresTransaction] & Persistence[PostgresTransaction] & Migration, Env[DatabaseConfig] & Sync & Scope] =
    datasourceConfig.to(dataSource)
      .and(migrationConfig)
      .to(database.and(database.to(persistence)).and(migration))

  /**
   * Extracts the DatasourceConfig from the loaded DatabaseConfig. This allows
   * the HikariDataSource layer to depend only on the specific configuration it needs,
   * rather than the entire DatabaseConfig.
   */
  lazy val datasourceConfig: Layer[DatasourceConfig, Env[DatabaseConfig] & Sync] =
    Layer.from((dbConfig: DatabaseConfig) => dbConfig.datasource)

  /**
   * Extracts the MigrationConfig from the loaded DatabaseConfig. This allows
   * any migration-related layers to depend only on the specific configuration they need,
   * rather than the entire DatabaseConfig.
   */
  lazy val migrationConfig: Layer[MigrationConfig, Env[DatabaseConfig] & Sync] =
    Layer.from((dbConfig: DatabaseConfig) => dbConfig.migration)

  /**
   * Provides a HikariDataSource connection pool based on the provided DatasourceConfig.
   * The datasource is properly managed with acquisition and release to ensure that
   * connections are closed when no longer needed.
   */
  lazy val dataSource: Layer[HikariDataSource, Env[DatasourceConfig] & Sync & Scope] =
    Layer.from { (config: DatasourceConfig) =>
      Kyo.acquireRelease(HikariDataSource(config.toHikariConfig))(ds => ds.close())
    }

  /**
   * Provides a PostgresDatabase instance that implements the Database trait using
   * the HikariDataSource. This layer depends on the datasource and provides the
   * database service to the application.
   */
  lazy val database: Layer[Database[PostgresTransaction], Env[HikariDataSource]] =
    Layer.from { (ds: HikariDataSource) =>
      PostgresDatabase(ds): Database[PostgresTransaction]
    }

  /**
   * Provides a [[Migration]] instance configured with Flyway, using the
   * [[MigrationConfig]] for Flyway settings and [[HikariDataSource]] as the
   * database connection.
   */
  lazy val migration: Layer[Migration, Env[MigrationConfig] & Env[HikariDataSource]] =
    Layer {
      for
        config     <- Env.get[MigrationConfig]
        dataSource <- Env.get[HikariDataSource]
      yield
        val flyway = config.toFlywayConfig.dataSource(dataSource).load()
        Migration(flyway)
    }

  /**
   * Provides a Persistence instance that implements the Persistence trait using
   * Postgres repositories.
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
