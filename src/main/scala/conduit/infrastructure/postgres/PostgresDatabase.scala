package conduit.infrastructure.postgres

import com.zaxxer.hikari.HikariDataSource
import conduit.domain.error.ApplicationError
import conduit.domain.service.persistence.Database
import kyo.*

import java.sql.Connection

class PostgresDatabase(val datasource: HikariDataSource) extends Database[PostgresTransaction] {

  /**
   * Executes an effect within a database transaction context.
   *
   * This method takes an effect that requires access to a transaction (via Env[Tx])
   * and executes it atomically, returning the result as an effect that only requires Async.
   *
   * The transaction is automatically managed by the database implementation, including:
   * - Connection acquisition
   * - Transaction initialization
   * - Commit or rollback based on success or failure
   *
   * @tparam A      the type of value produced by the effect
   * @tparam Effect the effect types required by the operation (e.g., Async, Abort, etc.)
   * @param effect the effect to execute within the transaction context
   * @return the result of executing the effect wrapped in an Async effect
   */
  override def transaction[A, Effect <: Pending](effect: A < (Effect & Env[PostgresTransaction])): A < Effect =
    Scope.run:
      for {
        transaction <- Scope.acquireRelease(acquireTransaction)(releaseTransaction)
        result      <- Abort.run(Env.run(transaction)(effect))
        value       <- handleResult(transaction, result)
      } yield value

  /**
   * Acquires a new database transaction by obtaining a connection from the datasource.
   *
   * @return a PostgresTransaction wrapped in an effect that can fail with a connection error
   */
  private def acquireTransaction: PostgresTransaction < (Sync & Abort[PostgresTransaction.Error]) =
    Kyo.defer(Abort.catching(datasource.getConnection)).map { connection =>
      Abort
        .catching:
          connection.setAutoCommit(false)
          connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
          PostgresTransaction(connection)
        .mapAbort(PostgresTransaction.Error.ConnectionError.apply)
    }

  /**
   * Releases a database transaction by closing the underlying connection.
   *
   * @param transaction the transaction to be released
   * @return an effect that can fail with a connection error if closing the connection fails
   */
  private def releaseTransaction(transaction: PostgresTransaction): Unit < (Sync & Abort[Throwable]) =
    Kyo.attempt(transaction.dbTx.connection.close())

  /**
   * Handles the result of executing the effect within the transaction context.
   *
   * Depending on whether the result is a success, failure, or panic, this method
   * will commit or roll back the transaction accordingly and return the appropriate value or error.
   *
   * @param transaction the current database transaction
   * @param result the result of executing the effect, which can be a success, failure, or panic
   * @return the value produced by a successful execution or an effect that fails with an application error
   */
  private def handleResult[A](
    transaction: PostgresTransaction,
    result: Result[ApplicationError, A],
  ): A < (Sync & Abort[ApplicationError]) =
    result match {
      case Result.Success(value) => transaction.commit.map(_ => value)
      case Result.Failure(error) => transaction.rollback.map(_ => Abort.fail(error))
      case Result.Panic(reason)  => transaction.rollback.map(_ => Abort.panic(reason))
    }

}
