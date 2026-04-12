package conduit.domain.service.usecase

import conduit.domain.error.ApplicationError
import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.request.article.ListArticlesRequest
import conduit.domain.response.article.ArticleListResponse
import conduit.domain.service.persistence.{ ArticleRepository, Database, Persistence }
import kyo.*

import java.util.UUID

/**
 * Use case for listing articles with optional filtering and pagination.
 *
 * Supports filtering by tag, author, and favoriter. The requester may be
 * anonymous — in that case the response simply reports no favorites and no
 * followed authors, so the public feed remains fully browsable without an
 * account.
 */
class ListArticlesUseCase[Tx <: Database.Transaction](
  database: Database[Tx],
  persistence: Persistence[Tx],
) {
  private type Effect = Async & Abort[ApplicationError]

  /**
   * Executes the use case to list articles based on the provided request.
   *
   * @param request The request containing filters, pagination, and requester information.
   * @return An effect that produces an ArticleListResponse or an ApplicationError.
   */
  def apply(request: ListArticlesRequest): ArticleListResponse < Effect =
    database.transaction:
      val params = request.filters.map(toSearchParam)
      for {
        count     <- persistence.articles.searchCount(params)
        articles  <- persistence.articles.search(params, request.offset, request.limit)
        profiles  <- findAuthors(articles)
        favorites <- favoritesOf(request.requester, articles)
        followed  <- followedBy(request.requester, profiles)
      } yield ArticleListResponse.make(count, articles, profiles, favorites, followed)

  /**
   * Converts a ListArticlesRequest.Filter into an ArticleRepository.SearchParam.
   *
   * This is used to translate the filtering criteria from the request into
   * the format expected by the article repository for searching.
   *
   * @param filter The filter from the request to convert.
   * @return The corresponding SearchParam for the article repository.
   */
  private def toSearchParam(filter: ListArticlesRequest.Filter): ArticleRepository.SearchParam =
    filter match {
      case ListArticlesRequest.Filter.Tag(name)            => ArticleRepository.SearchParam.Tag(name)
      case ListArticlesRequest.Filter.Author(username)     => ArticleRepository.SearchParam.Author(username)
      case ListArticlesRequest.Filter.FavoriteOf(username) => ArticleRepository.SearchParam.FavoriteBy(username)
    }

  /**
   * Finds the authors of the given list of articles.
   *
   * This method retrieves the user profiles for the authors of the provided articles
   * by querying the persistence layer. It extracts the author IDs from the articles
   * and uses them to find the corresponding user profiles.
   *
   * @param articles The list of articles for which to find the authors.
   * @return An effect that produces a list of UserProfile instances for the authors.
   */
  private def findAuthors(articles: List[Article]): List[UserProfile] < (Effect & Env[Tx]) =
    persistence.users.findByUsers(articles.map(_.authorId))

  /**
   * Determines which articles are favorited by the requester.
   *
   * This method checks if the requester is authenticated and, if so, retrieves
   * the set of article IDs that the requester has favorited from the persistence layer.
   * If the requester is anonymous, it simply returns an empty set, indicating that
   * no articles are favorited.
   *
   * @param requester The user making the request, which may be anonymous or authenticated.
   * @param articles  The list of articles to check for favorites.
   * @return An effect that produces a set of article IDs that are favorited by the requester.
   */
  private def favoritesOf(requester: User, articles: List[Article]): Set[UUID] < (Effect & Env[Tx]) =
    requester match {
      case User.Anonymous          => Set.empty[UUID]
      case User.Authenticated(uid) => persistence.favorites.favoriteOf(uid, articles.map(_.id)).map(_.toSet)
    }

  /**
   * Determines which authors are followed by the requester.
   *
   * This method checks if the requester is authenticated and, if so, retrieves
   * the set of author IDs that the requester is following from the persistence layer.
   * If the requester is anonymous, it simply returns an empty set, indicating that
   * no authors are followed.
   *
   * @param requester The user making the request, which may be anonymous or authenticated.
   * @param authors   The list of author profiles to check for following status.
   * @return An effect that produces a set of author IDs that are followed by the requester.
   */
  private def followedBy(requester: User, authors: List[UserProfile]): Set[UUID] < (Effect & Env[Tx]) =
    requester match {
      case User.Anonymous          => Set.empty[UUID]
      case User.Authenticated(uid) => persistence.followers.followedBy(uid, authors.map(_.id)).map(_.toSet)
    }
}
