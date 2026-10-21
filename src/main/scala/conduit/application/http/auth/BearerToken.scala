package conduit.application.http.auth

import kyo.{ Absent, Maybe, Present }

type BearerToken = BearerToken.Type

object BearerToken {
  opaque type Type <: String = String
  def apply(value: String): BearerToken = value

  /**
   * Parses the `Authorization: Token <jwt>` scheme used by the RealWorld spec.
   * The prefix check is case-sensitive on the `Token` keyword, matching the
   * previous Tapir codec behaviour.
   *
   * Returns the extracted token when the header is present and well-formed,
   * `Absent` when the header is missing or does not use the `Token` scheme.
   */
  def fromHeader(header: Maybe[String]): Maybe[BearerToken] = header match
    case Absent       => Absent
    case Present(raw) =>
      raw.trim.split(" ", 2) match
        case Array("Token", token) => Present(BearerToken(token.trim))
        case _                     => Absent
}
