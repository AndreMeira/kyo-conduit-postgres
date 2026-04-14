package conduit.domain.service.usecase

import conduit.domain.error.MissingEntity.UserProfileMissing
import conduit.domain.error.{ ApplicationError, Unauthorised }
import conduit.domain.model.{ Credentials, User }
import conduit.domain.request.user.AuthenticateRequest
import conduit.domain.response.user.AuthenticationResponse
import conduit.domain.service.authentication.AuthenticationService
import conduit.domain.service.persistence.Database.Transaction
import conduit.domain.service.persistence.{ Database, Persistence }
import conduit.domain.service.validation.{ CredentialsInputValidation, StateValidationService }
import zio.prelude.Validation
import conduit.domain.syntax.*
import kyo.*

class UserAuthenticationUseCase[Tx <: Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
  authentication: AuthenticationService,
) {
  type Effect = Async & Abort[ApplicationError]

  def apply(request: AuthenticateRequest): AuthenticationResponse < Effect =
    database.transaction:
      for {
        credentials <- parse(request).validOrAbort
        hashed      <- authentication.hash(credentials)
        userId      <- authenticate(hashed)
        token       <- authentication.encodeToken(userId)
        profile     <- persistence.users.findByUser(userId) ?! UserProfileMissing(userId)
      } yield AuthenticationResponse.make(credentials.email, profile, token)

  private def parse(request: AuthenticateRequest): Validated[Credentials.Clear] < Any =
    Kyo.lift {
      Validation.validateWith(
        CredentialsInputValidation.email(request.payload.user.email),
        CredentialsInputValidation.password(request.payload.user.password),
      )(Credentials.Clear(_, _))
    }

  private def authenticate(credentials: Credentials.Hashed): User.Id < (Effect & Env[Tx]) =
    persistence.credentials.find(credentials).map {
      case Maybe.Present(userId) => userId
      case Maybe.Absent          => Abort.fail(Unauthorised.InvalidCredentials)
    }
}
