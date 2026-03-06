package conduit.infrastructure.postgres

import com.augustnagro.magnum.{ DbTx, MagnumInterop, SqlLogger }
import conduit.domain.error.ApplicationError
import conduit.domain.service.persistence.Database
import kyo.*

import java.sql.Connection
import java.time.Instant

/**
 * Represents a PostgreSQL database transaction.
 *
 * @param connection The JDBC connection used for the transaction.
 */
class PostgresTransaction(connection: Connection) extends Database.Transaction {

  /**
   * Provides a `DbTx` instance for interacting with the database.
   *
   * @return The `DbTx` instance.
   */
  def dbTx: DbTx = MagnumInterop.makeDbTx(connection)

  /**
   * Starts the transaction by disabling auto-commit and setting the isolation level.
   *
   * @return A computation that starts the transaction or aborts with a `ConnectionError`.
   */
  def start: Unit < (Sync & Abort[PostgresTransaction.Error]) =
    Kyo
      .attempt {
        connection.setAutoCommit(false)
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
      }
      .mapAbort(error => PostgresTransaction.Error.ConnectionError(error))

  /**
   * Commits the transaction.
   *
   * @return A computation that commits the transaction or aborts with a `TransactionError`.
   */
  def commit: Unit < (Sync & Abort[PostgresTransaction.Error]) =
    Kyo
      .attempt(connection.commit())
      .mapAbort(error => PostgresTransaction.Error.TransactionError(error))

  /**
   * Rolls back the transaction.
   *
   * @return A computation that rolls back the transaction or aborts with a `TransactionError`.
   */
  def rollback: Unit < (Sync & Abort[PostgresTransaction.Error]) =
    Kyo
      .attempt(connection.rollback())
      .mapAbort(error => PostgresTransaction.Error.TransactionError(error))
}

object PostgresTransaction {

  /**
   * Represents errors that can occur during a PostgreSQL transaction.
   */
  enum Error extends ApplicationError.VendorError:
    /**
     * Indicates a failure to establish a database connection.
     *
     * @param reason The underlying exception.
     */
    case ConnectionError(reason: Throwable)

    /**
     * Indicates a failure during a database transaction.
     *
     * @param reason The underlying exception.
     */
    case TransactionError(reason: Throwable)

    /**
     * Provides a human-readable error message.
     *
     * @return The error message.
     */
    override def message: String = this match {
      case ConnectionError(reason)  => s"Failed to establish database connection: ${reason.getMessage}"
      case TransactionError(reason) => s"Database transaction failed: ${reason.getMessage}"
    }

  object Transactional:

    /**
     * Executes a database action within a transaction.
     *
     * @param action The action to execute, which requires a `DbTx` context.
     * @tparam A The result type of the action.
     * @return The result of the action or an error if the transaction fails.
     */
    def apply[A](action: DbTx ?=> A): A < (Sync & Abort[Error] & Env[PostgresTransaction]) =
      Env.get[PostgresTransaction].map { transaction =>
        Kyo
          .attempt(action(using transaction.dbTx))
          .mapAbort(Error.TransactionError.apply)
      }

    /**
     * Executes a database action within a transaction, providing the current timestamp.
     *
     * @param action The action to execute, which requires a `DbTx` context and the current timestamp.
     * @tparam A The result type of the action.
     * @return The result of the action or an error if the transaction fails.
     */
    def withTime[A](action: DbTx ?=> Instant => A): A < (Sync & Abort[Error] & Env[PostgresTransaction]) =
      for {
        now    <- Clock.now.map(_.toJava)
        result <- apply(action(now))
      } yield result
}
