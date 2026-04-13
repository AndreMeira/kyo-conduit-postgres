package conduit.infrastructure.postgres.configuration

import conduit.infrastructure.configuration.ConfigurationLoader
import kyo.*
import pureconfig.ConfigReader

case class DatabaseConfig(
  datasource: DatasourceConfig,
  migration: MigrationConfig,
) derives ConfigReader
