package conduit.infrastructure.postgres.configuration

import conduit.infrastructure.configuration.ConfigurationLoader
import kyo.*
import pureconfig.ConfigReader

case class DatabaseConfig(
  datasource: DatasourceConfig
) derives ConfigReader

object DatabaseConfig {

  /**
   * Loads the database source configuration from the specified resource path.
   *
   * @return An effect that produces the loaded DatasourceConfig or an error if loading fails.
   */
  def load: DatabaseConfig < (Abort[ConfigurationLoader.Error] & Sync) = Kyo.defer {
    ConfigurationLoader.load[DatabaseConfig]("config/database.conf")
  }

}
