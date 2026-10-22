package conduit.infrastructure.postgres

import conduit.domain.error.ApplicationError
import conduit.domain.service.persistence.Database
import kyo.*

/**
 * The transaction a repository runs its statements in, provided by [[PostgresDatabase.transaction]].
 *
 * It holds no connection: the client's transaction pins its connection to the fiber, and a
 * statement run with this state, whose client is the one the transaction was opened on, uses
 * that pinned connection.
 *
 * @param state the client the enclosing transaction was opened on, and the config its statements run with
 */
case class PostgresTransaction(state: DB.State) extends Database.Transaction

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
        DB.run(tx.state)(action.mapAbort(cause => PostgresDatabase.Error.QueryError(cause)))
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
