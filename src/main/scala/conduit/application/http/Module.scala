package conduit.application.http

import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.Database
import conduit.domain.service.usecase.UseCases
import kyo.*

/** Kyo Layer definitions for the HTTP application layer.
  *
  * Provides a layer for [[HttpRoutes]], which depends on [[UseCases]] and
  * [[AuthenticationService]]. The layer is parameterised on the transaction
  * type so it can work with any storage backend (Postgres, in-memory, etc.).
  */
object Module:

  /** Provides [[HttpRoutes]] from [[UseCases]] and [[AuthenticationService]]. */
  def httpRoutes[Tx <: Database.Transaction: Tag]: Layer[HttpRoutes, Env[UseCases[Tx]] & Env[AuthenticationService]] =
    Layer {
      for
        useCases       <- Env.get[UseCases[Tx]]
        authentication <- Env.get[AuthenticationService]
      yield HttpRoutes(useCases, authentication)
    }
