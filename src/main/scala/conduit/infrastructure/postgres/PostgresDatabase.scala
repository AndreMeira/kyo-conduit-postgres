package conduit.infrastructure.postgres

import conduit.domain.error.ApplicationError
import conduit.domain.service.persistence.Database
import conduit.infrastructure.postgres.configuration.DatasourceConfig
import kyo.*

/**
 * Postgres implementation of [[Database]], backed by a kyo-sql [[PostgresClient]].
 *
 * Each call to `transaction` leases one connection from the client's pool and pins it to the
 * calling fiber for the duration of the effect, so every statement the effect runs, including
 * those of fibers it forks, shares the same transaction.
 *
 * @param client the pooled client transactions are opened on
 */
class PostgresDatabase(client: PostgresClient) extends Database[PostgresTransaction] {

  /**
   * Runs the effect inside a transaction, committing when it succeeds and rolling back when it
   * aborts or panics.
   *
   * A statement that failed inside the effect rolls the transaction back even when the effect
   * recovered from that failure.
   *
   * @tparam A      the type of value produced by the effect
   * @tparam Effect the effect types required by the operation
   * @param effect the effect to execute within the transaction context
   * @return the effect's result; aborts with the effect's own `ApplicationError`, or with
   *         `PostgresDatabase.Error.TransactionError` when beginning, committing or rolling back fails
   */
  def transaction[A, Effect <: Pending](effect: A < (Effect & Env[PostgresTransaction])): A < Effect =
    client
      .transaction(Env.run(PostgresTransaction(DB.State(client, client.config)))(effect))
      .mapAbort(cause => PostgresDatabase.Error.TransactionError(cause))
}

object PostgresDatabase {

  /**
   * Failures raised by the Postgres client, and writes that did not affect the rows they targeted.
   */
  enum Error extends ApplicationError.VendorError:

    /**
     * The connection pool could not be opened.
     *
     * @param cause the underlying exception
     */
    case PoolInitFailed(cause: SqlException)

    /**
     * A statement failed.
     *
     * @param cause the underlying exception
     */
    case QueryError(cause: SqlException)

    /**
     * Beginning, committing or rolling back a transaction failed.
     *
     * @param cause the underlying exception
     */
    case TransactionError(cause: SqlException)

    /**
     * A write affected a number of rows other than the one it targeted.
     *
     * @param table the table written to
     * @param affected the number of rows the write affected
     */
    case UnexpectedRowCount(table: String, affected: Long)

    /**
     * Provides a human-readable error message.
     *
     * @return the error message, including the underlying exception's message
     */
    override def message: String = this match
      case PoolInitFailed(cause)   => s"Failed to initialize the connection pool: ${cause.message}"
      case QueryError(cause)       => s"Query failed: ${cause.message}"
      case TransactionError(cause) => s"Transaction failed: ${cause.message}"
      case UnexpectedRowCount(table, affected) => s"Expected a write to $table to affect one row, it affected $affected"

  /**
   * Opens a pooled Postgres client for the configured database, closed when the enclosing Scope ends.
   *
   * @param config the host, port, database, credentials and pool settings to connect with
   * @return the open client; aborts with `Error.PoolInitFailed` when the pool cannot be opened
   */
  def client(config: DatasourceConfig): PostgresClient < (Async & Scope & Abort[Error]) = {
    val url = s"postgres://${config.user}:${config.password}@${config.host}:${config.port}/${config.database}"
    PostgresClient.init(url, config.toSqlConfig).mapAbort(cause => Error.PoolInitFailed(cause))
  }
}
