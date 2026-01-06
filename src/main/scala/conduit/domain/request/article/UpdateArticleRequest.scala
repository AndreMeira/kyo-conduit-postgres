package conduit.domain.request.article

import conduit.domain.model.{ Article, User }

case class UpdateArticleRequest(
  requester: User.Authenticated,
  slug: String,
  payload: UpdateArticleRequest.Payload,
)

object UpdateArticleRequest:
  case class Payload(article: Data) // wrapping due to spec

  // @todo replace Option with kyo.Maybe
  case class Data(
    title: Option[String],
    description: Option[String],
    body: Option[String],
  )
