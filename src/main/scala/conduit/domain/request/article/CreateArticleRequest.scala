package conduit.domain.request.article

import conduit.domain.model.User

case class CreateArticleRequest(
  requester: User.Authenticated,
  payload: CreateArticleRequest.Payload,
)

object CreateArticleRequest:
  case class Payload(article: Data) // Wraps the article data due to API spec

  case class Data(
    title: String,
    description: String,
    body: String,
    tagList: Option[List[String]], // @todo replace with kyo.Maybe
  ) {
    def tags: List[String] = tagList.getOrElse(Nil)
  }
