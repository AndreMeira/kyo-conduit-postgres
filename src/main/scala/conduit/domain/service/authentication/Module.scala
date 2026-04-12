package conduit.domain.service.authentication

import kyo.*

/**
 * Kyo Layer definitions for the authentication domain.
 *
 * Provides layers for [[AuthenticationService]] and its [[AuthenticationService.Config]].
 */
object Module:

  val authenticationService: Layer[AuthenticationService, Env[AuthenticationService.Config]] =
    Layer.from { (config: AuthenticationService.Config) =>
      AuthenticationService(Clock.live, config)
    }
