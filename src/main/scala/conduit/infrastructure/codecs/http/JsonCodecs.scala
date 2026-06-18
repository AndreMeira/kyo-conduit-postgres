package conduit.infrastructure.codecs.http

import conduit.domain.request.Patchable
import conduit.domain.request.article.UpdateArticleRequest
import conduit.domain.request.user.UpdateUserRequest
import conduit.domain.types.{ ArticleSlug, ProfileName, TagName }
import io.circe.{ Decoder, Encoder, HCursor }
import kyo.HttpCodec

/**
 * Custom Circe decoders for request types that need to distinguish between
 * JSON `null` and missing fields.
 *
 * These decoders take priority over `circe-generic-auto` derived decoders
 * because they are defined as explicit givens in scope.
 */
object JsonCodecs:

  /** Circe codec — treat Patchable[A] as optional A. */
  given [A: Encoder]: Encoder[Patchable[A]] = Encoder[Option[A]].contramap:
    case Patchable.Present(value) => Some(value)
    case _                        => None

  /**
   * Circe decoder for [[UpdateUserRequest.Data]], reading each field as [[Patchable]]
   * to distinguish JSON `null` (clear the field) from absent (leave unchanged).
   */
  given updateUserRequest: Decoder[UpdateUserRequest.Data] = Decoder.instance { cursor =>
    for
      email    <- cursor.patchable[String]("email")
      username <- cursor.patchable[String]("username")
      password <- cursor.patchable[String]("password")
      bio      <- cursor.patchable[String]("bio")
      image    <- cursor.patchable[String]("image")
    yield UpdateUserRequest.Data(email, username, password, bio, image)
  }

  /**
   * Circe decoder for [[UpdateArticleRequest.Data]], reading `tagList` as [[Patchable]]
   * and `title`, `description`, `body` as plain `Option` (optional patch fields).
   */
  given updateArticleRequest: Decoder[UpdateArticleRequest.Data] = Decoder.instance { cursor =>
    for
      title       <- cursor.get[Option[String]]("title")
      description <- cursor.get[Option[String]]("description")
      body        <- cursor.get[Option[String]]("body")
      tagList     <- cursor.patchable[List[String]]("tagList")
    yield UpdateArticleRequest.Data(title, description, body, tagList)
  }

  /** kyo-http path/query codec for [[ProfileName]]. */
  given HttpCodec[ProfileName] = HttpCodec(profileName => profileName, raw => ProfileName(raw))

  /** kyo-http path/query codec for [[ArticleSlug]]. */
  given HttpCodec[ArticleSlug] = HttpCodec(articleSlug => articleSlug, raw => ArticleSlug(raw))

  /** kyo-http path/query codec for [[TagName]]. */
  given HttpCodec[TagName] = HttpCodec(tagName => tagName, raw => TagName(raw))

  /** Maps `Present` to `Some` and absent/empty to `None`. */
  private def toOption[A](p: Patchable[A]): Option[A] = p match
    case Patchable.Present(value) => Some(value)
    case _                        => None

  extension (cursor: HCursor)
    /** Decodes `field` as [[Patchable]]: present value → `Present`, explicit null → `Emtpy`, absent → `Absent`. */
    private def patchable[A](field: String)(using d: Decoder[A]): Decoder.Result[Patchable[A]] =
      cursor
        .get[Option[A]](field)
        .map:
          case Some(value) => Patchable.Present(value)
          case None        => if cursor.downField(field).succeeded then Patchable.Emtpy else Patchable.Absent
