package conduit.infrastructure.configuration

import conduit.domain.service.authentication.AuthenticationService
import conduit.infrastructure.postgres.configuration.DatabaseConfig
import kyo.*
import pureconfig.ConfigReader

object Module {

  given ConfigReader[AuthenticationService.Config] = ConfigReader.derived
  given ConfigReader[DatabaseConfig]               = ConfigReader.derived

  lazy val all: Layer[AuthenticationService.Config & DatabaseConfig, Abort[ConfigurationLoader.Error] & Sync] =
    authentication.and(database)

  /**
   * Layer definitions for each component of the Postgres infrastructure.
   * These can be used individually if only specific services are needed.
   */
  lazy val database: Layer[DatabaseConfig, Abort[ConfigurationLoader.Error] & Sync] = Layer:
    ConfigurationLoader.load[DatabaseConfig]("config/database.conf")

  /**
   * Layer definition for the AuthenticationService configuration. This layer can be
   * used to provide the necessary configuration for the authentication service in
   * the application.
   */
  lazy val authentication: Layer[AuthenticationService.Config, Abort[ConfigurationLoader.Error] & Sync] = Layer:
    ConfigurationLoader.load[AuthenticationService.Config]("config/authentication.conf")
}
