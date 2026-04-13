package conduit.infrastructure.postgres.configuration

import conduit.infrastructure.configuration.ConfigurationLoader
import kyo.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import pureconfig.ConfigReader

import scala.jdk.CollectionConverters.*
import scala.util.chaining.scalaUtilChainingOps

/**
 * Configuration for database migrations using Flyway.
 * This class encapsulates all necessary settings for running database migrations, 
 * including the initial SQL command, migration script locations, parameters, placeholders, 
 * and whether to allow cleaning the database.
 *
 * @param initSql The SQL command to execute before running any migrations (e.g., setting the search path).
 * @param locations A list of locations where Flyway should look for migration scripts (e.g., "classpath:db/migration").
 * @param parameters A map of configuration parameters to pass to Flyway (optional).
 * @param placeholders A map of placeholders to be replaced in migration scripts (optional).
 * @param allowClean A flag indicating whether Flyway should allow cleaning the database (default is false).
 */
case class MigrationConfig(
  initSql: String,
  locations: List[String],
  parameters: Map[String, String] = Map.empty,
  placeholders: Map[String, String] = Map.empty,
  allowClean: Boolean = false,
) derives ConfigReader {

  /**
   * Converts this MigrationConfig into a FluentConfiguration for Flyway.
   * This method sets up the Flyway configuration according to the properties of this MigrationConfig,
   * including the initial SQL command, migration locations, parameters, placeholders, and clean settings.
   *
   * @return A FluentConfiguration instance configured according to the properties of this MigrationConfig.
   */
  def toFlywayConfig: FluentConfiguration =
    Flyway
      .configure()
      .initSql(initSql)
      .locations(locations*)
      .cleanDisabled(!allowClean)
      .loggers("slf4j")
      .pipe: builder =>
        if parameters.nonEmpty
        then builder.configuration(parameters.asJava)
        else builder
      .pipe: builder =>
        if placeholders.nonEmpty
        then builder.placeholders(placeholders.asJava)
        else builder
}