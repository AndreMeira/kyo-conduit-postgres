package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.request.article.ArticleFeedRequest
import conduit.domain.response.article.ArticleListResponse
import conduit.domain.service.persistence.{ Database, Persistence }
import kyo.*

class ArticleFeedUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  def apply(request: ArticleFeedRequest): ArticleListResponse < Effect =
    database.transaction:
      for {
        count     <- count(request)
        articles  <- findArticles(request)
        profiles  <- findAuthors(articles)
        favorites <- favorites(request.requester.userId, articles)
        followed  <- followed(request.requester.userId, profiles)
      } yield ArticleListResponse.make(count, articles, profiles, favorites.toSet, followed.toSet)

  private def count(request: ArticleFeedRequest): Int < (Effect & Env[Tx]) =
    persistence.articles.countFeedOf(request.requester.userId)

  private def findArticles(request: ArticleFeedRequest): List[Article] < (Effect & Env[Tx]) =
    persistence.articles.feedOf(request.requester.userId, request.offset, request.limit)

  private def findAuthors(articles: List[Article]): List[UserProfile] < (Effect & Env[Tx]) =
    persistence.users.findByUsers(articles.map(_.authorId))

  private def favorites(userId: User.Id, articles: List[Article]): List[Article.Id] < (Effect & Env[Tx]) =
    persistence.favorites.favoriteOf(userId, articles.map(_.id))

  private def followed(userId: User.Id, authors: List[UserProfile]): List[UserProfile.Id] < (Effect & Env[Tx]) =
    persistence.followers.followedBy(userId, authors.map(_.id))
}
