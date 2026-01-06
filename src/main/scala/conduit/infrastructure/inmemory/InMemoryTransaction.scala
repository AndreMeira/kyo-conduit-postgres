package conduit.infrastructure.inmemory

import conduit.domain.service.persistence.Database.Transaction
import kyo.*

class InMemoryTransaction(val state: InMemoryState) extends Transaction

object InMemoryTransaction:
  def apply[A, Effect](effect: InMemoryState => A < Effect): A < (Effect & Env[InMemoryTransaction]) =
    Env.get[InMemoryTransaction].map(tx => effect(tx.state))
