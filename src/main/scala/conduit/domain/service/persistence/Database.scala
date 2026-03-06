package conduit.domain.service.persistence

import conduit.domain.error.ApplicationError
import Database.Transaction
import kyo.*

/**
 * Abstract representation of a database capable of managing transactions.
 *
 * This trait defines the contract for executing effects within a transactional
 * context. Implementations of this trait are responsible for handling the
 * lifecycle of database transactions, including starting, committing, and
 * rolling back transactions as necessary.
 *
 * @tparam Tx the transaction type used for database operations
 */
trait Database[Tx <: Transaction] {
  
  /** 
   * Type alias representing the combined effect requirements for pending operations.
   *
   * This alias combines the Async effect with the Abort effect for ApplicationError,
   * indicating that operations may be asynchronous and can fail with application-level errors.
   */
  type Pending = Async & Abort[ApplicationError]
  
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
   * @tparam A the type of value produced by the effect
   * @tparam Effect the effect types required by the operation (e.g., Async, Abort, etc.)
   * @param effect the effect to execute within the transaction context
   * @return the result of executing the effect wrapped in an Async effect
   */
  def transaction[A, Effect <: Pending](effect: A < (Effect & Env[Tx])): A < Effect
}

object Database:
  /**
   * Nominal type representing a database transaction.
   *
   * This is a marker trait used for type-level distinction and dependency injection.
   * It has no runtime methods and serves purely for compile-time type safety when
   * passing transaction contexts through the environment.
   */
  trait Transaction