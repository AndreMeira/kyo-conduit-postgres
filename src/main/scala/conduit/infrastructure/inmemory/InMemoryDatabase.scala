package conduit.infrastructure.inmemory

import conduit.domain.service.persistence.Database
import kyo.*

/**
 * In-memory implementation of the Database for managing transactions.
 *
 * This class provides a way to execute effects within a transaction context
 * using an in-memory state. It leverages the Kyo library to handle effects
 * and environment management.
 *
 * @param state the in-memory state used for transactions
 */
class InMemoryDatabase(state: InMemoryState) extends Database[InMemoryTransaction] {

  /**
   * Executes a given effect within a transaction context.
   *
   * @param effect the effect to be executed within the transaction
   * @return the result of the effect execution
   */
  override def transaction[A, Effect <: Pending](
    effect: A < (Effect & Env[InMemoryTransaction])
  ): A < Effect =
    for {
      copied <- state.duplicate
      tx      = new InMemoryTransaction(copied)
      result <- Env.run(tx)(effect)
      _      <- state.merge(tx.state)
    } yield result

}
