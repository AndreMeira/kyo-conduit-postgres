package conduit.domain.response.article

import conduit.domain.model.{Article, UserProfile}
import conduit.domain.response.user.GetProfileResponse

import java.util.UUID

case class ArticleListResponse(articles: List[ArticleListResponse.Payload], articlesCount: Int)

object ArticleListResponse:
  case class Payload(
    slug: String,
    title: String,
    description: String,
    tagList: List[String],
    createdAt: String,
    updatedAt: String,
    favorited: Boolean,
    favoritesCount: Int,
    author: GetProfileResponse.Payload,
  )

  def make(
    count: Int,
    articles: List[Article],
    profiles: List[UserProfile],
    favorites: Set[UUID],
    followed: Set[UUID],
  ): ArticleListResponse = {
    val profilesById = profiles.map(profile => profile.id -> profile).toMap
    ArticleListResponse(
      articles =
        for {
          article <- articles
          author  <- profilesById.get(article.authorId)
        } yield Payload(
          slug = article.slug,
          title = article.title,
          description = article.description,
          tagList = article.tags,
          createdAt = article.createdAt.toString,
          updatedAt = article.updatedAt.toString,
          favoritesCount = article.favoriteCount,
          favorited = favorites.contains(article.id),
          author = GetProfileResponse.payload(author, followed.contains(author.id)),
        ),
      articlesCount = count,
    )
  }
