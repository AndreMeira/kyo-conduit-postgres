package conduit.domain.response.article

import conduit.domain.model.{Article, UserProfile}
import conduit.domain.response.user.GetProfileResponse

case class GetArticleResponse(article: GetArticleResponse.Payload)

object GetArticleResponse:
  case class Payload(
    slug: String,
    title: String,
    description: String,
    body: String,
    tagList: List[String],
    createdAt: String,
    updatedAt: String,
    favorited: Boolean,
    favoritesCount: Int,
    author: GetProfileResponse.Payload,
  )

  def make(
    article: Article,
    profile: UserProfile,
    favorited: Boolean,
    following: Boolean,
  ): GetArticleResponse = GetArticleResponse(
    Payload(
      slug = article.slug,
      title = article.title,
      description = article.description,
      body = article.body,
      tagList = article.tags,
      createdAt = article.createdAt.toString,
      updatedAt = article.updatedAt.toString,
      favorited = favorited,
      favoritesCount = article.favoriteCount,
      author = GetProfileResponse.payload(profile, following),
    )
  )
