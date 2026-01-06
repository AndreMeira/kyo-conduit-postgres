package conduit.infrastructure.inmemory

import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.model.Article.Id
import conduit.domain.service.persistence.ArticleRepository
import conduit.domain.service.persistence.ArticleRepository.SearchParam
import kyo.*

/**
 * In-memory implementation of the ArticleRepository for the Conduit application.
 *
 * This repository stores articles in memory and provides operations for finding, saving,
 * updating, and searching articles. All operations are wrapped in InMemoryTransaction
 * to ensure consistent access to the shared article state.
 */
class InMemoryArticleRepository extends ArticleRepository[InMemoryTransaction] {
  /**
   * Finds an article by its ID.
   *
   * @param id the article ID to search for
   * @return a Maybe containing the article if found, or None otherwise
   */
  override def find(id: Id): Maybe[Article] < Effect =
    InMemoryTransaction { state =>
      state.articles.get.map(_.get(id)).map(Maybe.fromOption)
    }

  /**
   * Checks if an article with the given ID exists.
   *
   * @param id the article ID to check
   * @return true if the article exists, false otherwise
   */
  override def exists(id: Id): Boolean < Effect =
    InMemoryTransaction { state =>
      state.articles.get.map(_.contains(id))
    }

  /**
   * Saves a new article to the repository.
   *
   * @param article the article to save
   * @return Unit
   */
  override def save(article: Article): Unit < Effect =
    InMemoryTransaction { state =>
      state.articles.updateAndGet(_ + (article.id -> article)).unit
    }

  /**
   * Updates an existing article in the repository.
   *
   * @param article the article to update
   * @return Unit
   */
  override def update(article: Article): Unit < Effect =
    InMemoryTransaction { state =>
      state.articles.updateAndGet(_ + (article.id -> article)).unit
    }

  /**
   * Searches for articles based on the provided search parameters.
   *
   * Supports filtering by tag, author, and favorite user. Multiple parameters
   * are applied as cumulative filters.
   *
   * @param params the search parameters to apply
   * @return a list of articles matching all search criteria
   */
  override def search(params: List[ArticleRepository.SearchParam]): List[Article] < Effect =
    InMemoryTransaction { state =>
      state.articles.get.map(articles => filter(articles.values.toList, params))
    }

  /**
   * Recursively applies search parameter filters to a list of articles.
   *
   * @param articles the articles to filter
   * @param params the search parameters to apply
   * @return a filtered list of articles
   */
  private def filter(articles: List[Article], params: List[SearchParam]): List[Article] < Effect =
    Loop(articles -> params) { (articles, params) =>
      params match {
        case (p: SearchParam.Tag) :: tail        => filter(articles, p).map(_ -> tail).map(Loop.continue)
        case (p: SearchParam.Author) :: tail     => filter(articles, p).map(_ -> tail).map(Loop.continue)
        case (p: SearchParam.FavoriteBy) :: tail => filter(articles, p).map(_ -> tail).map(Loop.continue)
        case _                                   => Loop.done(articles)
      }
    }

  /**
   * Filters articles by a specific tag.
   *
   * @param articles the articles to filter
   * @param tag the tag search parameter
   * @return articles containing the specified tag
   */
  private def filter(articles: List[Article], tag: SearchParam.Tag): List[Article] < Any =
    articles.filter(article => article.tags.contains(tag))

  /**
   * Filters articles by author name.
   *
   * @param articles the articles to filter
   * @param author the author search parameter
   * @return articles written by the specified author
   */
  private def filter(articles: List[Article], author: SearchParam.Author): List[Article] < Effect =
    InMemoryTransaction { state =>
      for {
        byAuthorName <- state.articlesByAuthorName
      } yield articles.filter { article =>
        val articleIds = byAuthorName.getOrElse(author.username, Nil).map(_.id)
        articleIds.contains(article.id)
      }
    }

  /**
   * Filters articles by a user's favorites.
   *
   * @param articles the articles to filter
   * @param user the favorite user search parameter
   * @return articles favorited by the specified user
   */
  private def filter(articles: List[Article], user: SearchParam.FavoriteBy): List[Article] < Effect =
    InMemoryTransaction { state =>
      for {
        favorites <- state.favoriteByUsername
      } yield articles.filter { article =>
        val articleIds = favorites.getOrElse(user.username, Nil)
        articleIds.contains(article.id)
      }
    }
}
