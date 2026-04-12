package conduit.infrastructure.postgres

import com.zaxxer.hikari.HikariDataSource
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.infrastructure.configuration.ConfigurationLoader
import conduit.infrastructure.postgres.configuration.{ DatabaseConfig, DatasourceConfig }
import kyo.*

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
  lazy val all: Layer[
    Persistence[PostgresTransaction] & Database[PostgresTransaction],
    Abort[ConfigurationLoader.Error] & Sync & Scope,
  ] = Layer.init[Persistence[PostgresTransaction] & Database[PostgresTransaction]](
    config,
    datasourceConfig,
    dataSource,
    database,
    persistence,
  )

  /**
   * Layer definitions for each component of the Postgres infrastructure.
   * These can be used individually if only specific services are needed.
   */
  lazy val config: Layer[DatabaseConfig, Abort[ConfigurationLoader.Error] & Sync] =
    Layer(DatabaseConfig.load)

  /**
   * Extracts the DatasourceConfig from the loaded DatabaseConfig. This allows
   * the HikariDataSource layer to depend only on the specific configuration it needs,
   * rather than the entire DatabaseConfig.
   */
  lazy val datasourceConfig: Layer[DatasourceConfig, Env[DatabaseConfig] & Sync] =
    Layer.from((dbConfig: DatabaseConfig) => dbConfig.datasource)

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
   * Provides a Persistence instance that implements the Persistence trait using
   * Postgres repositories. This layer depends on the database and provides the
   * persistence service to the application.
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
