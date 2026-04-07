package conduit.infrastructure.postgres

import kyo.*
import conduit.domain.error.ApplicationError
import conduit.infrastructure.postgres.Migration.Error.MigrationFailed
import org.flywaydb.core.Flyway

/**
 * Class responsible for applying and cleaning database migrations using Flyway.
 */
class Migration(flyway: Flyway) {

  /**
   * Applies the database migrations.
   * Logs the result and handles any errors that may occur during the migration process.
   *
   * @return A unit effect that represents the completion of the migration process, or an error if the migration fails.
   */
  def applyMigrations: Unit < (Sync & Abort[Migration.Error]) = Kyo.defer {
    Kyo
      .attempt(flyway.migrate())
      .tap(result => Kyo.logInfo(result.toString))
      .mapAbort(MigrationFailed(_))
      .unit
  }

  /**
   * Cleans the database migrations.
   * Logs the result and handles any errors that may occur during the cleaning process.
   *
   * @return A unit effect that represents the completion of the cleaning process, or an error if the cleaning fails.
   */
  def cleanMigrations: Unit < (Sync & Abort[ApplicationError]) = Kyo.defer {
    Kyo
      .attempt(flyway.clean())
      .tap(result => Kyo.logInfo(result.toString))
      .mapAbort(MigrationFailed(_))
      .unit
  }
}

object Migration:
  /**
   * Defines the error types that can occur during the migration process.
   * Each error type includes a message that describes the error and the cause of the error.
   */
  enum Error extends ApplicationError:
    case CleanFailed(cause: Throwable)
    case MigrationFailed(cause: Throwable)

    /**
     * Provides a message for each error type, including the cause of the error.
     * This message can be used for logging or displaying error information to the user.
     *
     * @return A string message that describes the error and its cause.
     */
    override def message: String = this match
      case CleanFailed(cause)     => s"Failed to clean migrations: ${cause.getMessage}"
      case MigrationFailed(cause) => s"Failed to apply migrations: ${cause.getMessage}"
