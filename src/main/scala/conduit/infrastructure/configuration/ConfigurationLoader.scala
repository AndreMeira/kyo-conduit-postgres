package conduit.infrastructure.configuration

import conduit.domain.error.ApplicationError
import kyo.*
import pureconfig.{ConfigReader, ConfigSource}

/** Utility for loading configuration files using PureConfig and Kyo for error handling. */
object ConfigurationLoader {
  
  /**
   * Loads a configuration of type A from the specified resource path.
   * Uses PureConfig to read the configuration and Kyo to handle errors.
   *
   * @param path The resource path to the configuration file.
   * @tparam A The type of the configuration to load, which must have a ConfigReader and Tag instance.
   * @return An effect that produces the loaded configuration or an error if loading fails.
   */
  def load[A: {ConfigReader, Tag}](path: String): A < Abort[Error] =
    Kyo
      .fromEither(ConfigSource.resources(path).load[A])
      .mapAbort(err => Error(path, err))

  /** 
   * Represents an error that occurs when loading a configuration file. 
   * Contains the path of the configuration and details about the error.
   * 
   * @param path The resource path of the configuration that failed to load.
   * @param details Additional details about the error that occurred during loading.
   */
  case class Error(path: String, details: Any) extends ApplicationError:
    override def message: String = s"Failed to load configuration from $path: $details"
}
