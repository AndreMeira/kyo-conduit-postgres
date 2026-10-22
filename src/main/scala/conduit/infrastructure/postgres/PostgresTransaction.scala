package conduit.infrastructure.postgres

import conduit.domain.error.ApplicationError
import conduit.domain.service.persistence.Database
import kyo.*

/**
 * The transaction a repository runs its statements in, provided by [[PostgresDatabase.transaction]].
 *
 * It holds the client rather than a connection: the connection is pinned to the fiber by the
 * client's own transaction, and statements run through this client inside it use that connection.
 *
 * @param client the client the enclosing transaction was opened on
 */
case class PostgresTransaction(client: PostgresClient) extends Database.Transaction

object PostgresTransaction:

  /**
   * Runs repository statements in the transaction found in the environment.
   */
  object Transactional:

    /**
     * The effects of a repository operation: asynchronous, failing with an `ApplicationError`,
     * and requiring the current transaction.
     */
    type Pending = Async & Abort[ApplicationError] & Env[PostgresTransaction]

    /**
     * Runs a database action in the current transaction.
     *
     * @param action the statements to run
     * @tparam A the result type of the action
     * @return the action's result; aborts with `PostgresDatabase.Error.QueryError` when a statement fails
     */
    def apply[A](action: A < (DB & Abort[SqlException])): A < Pending =
      Env.use[PostgresTransaction] { tx =>
        DB.run(tx.client)(action.mapAbort(cause => PostgresDatabase.Error.QueryError(cause)))
      }

    /**
     * Runs a database action in the current transaction, providing it the current time.
     *
     * @param action the statements to run, given the time read when the action starts
     * @tparam A the result type of the action
     * @return the action's result; aborts with `PostgresDatabase.Error.QueryError` when a statement fails
     */
    def withTime[A](action: Instant => A < (DB & Abort[SqlException])): A < Pending =
      Clock.now.map(now => apply(action(now)))
