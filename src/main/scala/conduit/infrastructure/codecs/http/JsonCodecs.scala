package conduit.infrastructure.codecs.http

import conduit.domain.request.Patchable
import conduit.domain.request.article.UpdateArticleRequest
import conduit.domain.request.user.UpdateUserRequest
import io.circe.{ Decoder, Encoder, HCursor }
import sttp.tapir.Schema

/** Custom Circe decoders for request types that need to distinguish between
  * JSON `null` and missing fields.
  *
  * These decoders take priority over `circe-generic-auto` derived decoders
  * because they are defined as explicit givens in scope.
  */
object JsonCodecs:

  /** Tapir schema — treat Patchable[A] as optional A for OpenAPI documentation. */
  given [A](using s: Schema[A]): Schema[Patchable[A]] =
    s.asOption.map(option => option.map(Patchable.Present(_))):
      case Patchable.Present(value) => Some(value)
      case _                        => None

  given [A: Encoder]: Encoder[Patchable[A]] = Encoder[Option[A]].contramap:
    case Patchable.Present(value) => Some(value)
    case _                        => None

  private def toOption[A](p: Patchable[A]): Option[A] = p match
    case Patchable.Present(value) => Some(value)
    case _                        => None

  given updateUserDataDecoder: Decoder[UpdateUserRequest.Data] = Decoder.instance { cursor =>
    for
      email    <- cursor.patchable[String]("email")
      username <- cursor.patchable[String]("username")
      password <- cursor.patchable[String]("password")
      bio      <- cursor.patchable[String]("bio")
      image    <- cursor.patchable[String]("image")
    yield UpdateUserRequest.Data(email, username, password, bio, image)
  }

  given updateArticleDataDecoder: Decoder[UpdateArticleRequest.Data] = Decoder.instance { cursor =>
    for
      title       <- cursor.get[Option[String]]("title")
      description <- cursor.get[Option[String]]("description")
      body        <- cursor.get[Option[String]]("body")
      tagList     <- cursor.patchable[List[String]]("tagList")
    yield UpdateArticleRequest.Data(title, description, body, tagList)
  }

  extension (cursor: HCursor)
    private def patchable[A](field: String)(using d: Decoder[A]): Decoder.Result[Patchable[A]] =
      cursor
        .get[Option[A]](field)
        .map:
          case Some(value) => Patchable.Present(value)
          case None        => if cursor.downField(field).succeeded then Patchable.Emtpy else Patchable.Absent
