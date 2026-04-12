package conduit.domain.service.validation

import conduit.domain.service.persistence.{ Database, Persistence }
import kyo.*

/**
 * Kyo Layer definitions for the validation domain.
 *
 * Provides a layer for [[StateValidationService]], parameterised on the
 * transaction type so it works with any storage backend.
 */
object Module:

  def stateValidationService[Tx <: Database.Transaction: Tag]
    : Layer[StateValidationService[Tx], Env[Persistence[Tx]]] =
    Layer.from { (persistence: Persistence[Tx]) =>
      StateValidationService(persistence)
    }
