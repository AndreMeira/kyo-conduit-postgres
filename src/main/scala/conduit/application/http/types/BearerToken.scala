package conduit.application.http.types

import sttp.tapir.Codec
import sttp.tapir.CodecFormat.TextPlain

type BearerToken = BearerToken.Type
object BearerToken {
  opaque type Type <: String = String
  def apply(value: String): BearerToken = value

  given Codec[String, BearerToken, TextPlain] =
    Codec.string.map(BearerToken(_))(identity)
}
