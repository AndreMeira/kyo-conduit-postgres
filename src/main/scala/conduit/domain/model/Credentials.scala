package conduit.domain.model

import java.util.UUID

enum Credentials:
  case Clear(email: String, password: String)
  case Hashed(userId: UUID, email: String, password: String)
  
object Credentials:
  type Email = String
