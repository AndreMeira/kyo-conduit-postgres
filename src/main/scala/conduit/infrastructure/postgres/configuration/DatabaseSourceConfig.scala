package conduit.infrastructure.postgres.configuration

import com.zaxxer.hikari.HikariConfig
import conduit.infrastructure.configuration.ConfigurationLoader
import kyo.*
import pureconfig.ConfigReader

import scala.concurrent.duration.Duration

/**
 * Configuration for the database connection pool.
 * This class holds all necessary information to establish a connection to the database,
 * including JDBC URL, credentials, pool settings, and driver information.
 *
 * @param jdbcUrl The JDBC URL for the database connection.
 * @param user The username for the database connection.
 * @param password The password for the database connection.
 * @param poolSize The size of the connection pool (default is 10).
 * @param connectionTestQuery The SQL query used to test the connection (default is "SELECT 1").
 * @param driverClass The fully qualified class name of the JDBC driver (default is "org.postgresql.Driver").
 * @param connectionTimeout The maximum time to wait for a connection from the pool (default is 60 seconds).
 */
case class DatabaseSourceConfig(
  jdbcUrl: String,
  user: String,
  password: String,
  poolSize: Int = 10,
  connectionTestQuery: String = "SELECT 1",
  driverClass: String = "org.postgresql.Driver",
  connectionTimeout: Duration = Duration.create(60, "seconds"),
) derives ConfigReader {

  /** 
   * Converts this DatabaseSourceConfig into a HikariConfig, which can be used to create a Hikari DataSource. 
   * This method maps the properties of DatabaseSourceConfig to the corresponding settings in HikariConfig.
   * Auto-commit is disabled by default to allow transaction management at application level.
   *
   * @return A HikariConfig instance configured according to the properties of this DatabaseSourceConfig.
   */
  def toHikariConfig: HikariConfig =
    val config = new HikariConfig()
    config.setJdbcUrl(jdbcUrl)
    config.setUsername(user)
    config.setPassword(password)
    config.setMinimumIdle(poolSize)
    config.setMaximumPoolSize(poolSize * 2)
    config.setConnectionTestQuery(connectionTestQuery)
    config.setDriverClassName(driverClass)
    config.setConnectionTimeout(connectionTimeout.toMillis)
    config.setAutoCommit(false)
    config
}

object DatabaseSourceConfig:

  /**
   * Loads the database source configuration from the specified resource path.
   *
   * @return An effect that produces the loaded DatabaseSourceConfig or an error if loading fails.
   */
  def load: DatabaseSourceConfig < Abort[ConfigurationLoader.Error] =
    ConfigurationLoader.load[DatabaseSourceConfig]("config/database.conf")
