package conduit.application.http.auth

import sttp.tapir.{ Codec, DecodeResult }
import sttp.tapir.CodecFormat.TextPlain

type BearerToken = BearerToken.Type
object BearerToken {
  opaque type Type <: String = String
  def apply(value: String): BearerToken = value

  /**
   * Tapir codec that decodes the `Authorization: Token <jwt>` scheme
   * used by the RealWorld spec. The prefix check is case-insensitive.
   *
   * Encoding prepends the `Token ` prefix back for round-tripping.
   */
  given Codec[String, BearerToken, TextPlain] =
    Codec.string.mapDecode { raw =>
      raw.trim.split(" ", 2) match
        case Array("Token", token) => DecodeResult.Value(BearerToken(token.trim))
        case _                     => DecodeResult.Mismatch("Token <jwt>", raw)
    }(token => s"Token $token")
}
