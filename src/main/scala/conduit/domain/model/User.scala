package conduit.domain.model

import conduit.domain.syntax.*

import java.util.UUID

enum User:
  case Anonymous
  case Authenticated(userId: UUID)

object User:
  type Id = UUID
  case class SignedToken(value: String)
