package conduit.infrastructure.inmemory

import conduit.domain.model.Article.Id
import conduit.domain.model.{ Article, UserProfile }
import conduit.domain.service.persistence.ArticleRepository
import conduit.domain.service.persistence.ArticleRepository.SearchParam
import conduit.infrastructure.inmemory.InMemoryState.Changed.{ Inserted, Updated }
import conduit.infrastructure.inmemory.InMemoryState.Failure
import conduit.infrastructure.inmemory.InMemoryState.RowReference.ArticleRow
import kyo.*

import java.time.Instant

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
   * Saves a new article to the repository using article data.
   *
   * @param article the article data to save
   * @return Unit
   */
  override def save(article: Article.Data): Unit < Effect =
    InMemoryTransaction { state =>
      val created = article.toArticle(favoriteCount = 0, tags = List.empty)
      state.articles.updateAndGet(_ + (article.id -> created))
        *> state.addChange(Inserted(ArticleRow(article.id)))
    }

  /**
   * Updates an existing article in the repository using article data.
   *
   * Preserves the existing favorite count and tags while updating
   * the core content fields.
   *
   * @param article the article data with updated content
   * @return Unit on successful update
   */
  override def update(article: Article.Data): Unit < Effect =
    InMemoryTransaction { state =>
      state.articles.get.map(_.get(article.id)).flatMap {
        case None           => Abort.fail(Failure.ConstraintViolation)
        case Some(existing) =>
          val updated = article.toArticle(existing.favoriteCount, existing.tags)
          state.articles.updateAndGet(_ + (article.id -> updated))
            *> state.addChange(Updated(ArticleRow(article.id)))
      }
    }

  /**
   * Deletes an article from the repository.
   *
   * @param id the ID of the article to delete
   * @return Unit
   */
  override def delete(id: Article.Id): Unit < Effect =
    InMemoryTransaction { state =>
      state.articles.updateAndGet(_ - id)
        *> state.addChange(InMemoryState.Changed.Deleted(ArticleRow(id)))
    }

  /**
   * Searches for articles based on the provided search parameters.
   *
   * Supports filtering by tag, author, and favorite user. Multiple parameters
   * are applied as cumulative filters (intersection).
   *
   * @param params the search parameters to apply
   * @return a list of articles matching all search criteria
   */
  override def search(params: List[ArticleRepository.SearchParam], offset: Int, limit: Int): List[Article] < Effect =
    InMemoryTransaction { state =>
      state.articles.get
        .map(articles => filter(articles.values.toList, params))
        .map(_.sortBy(_.createdAt)(using Ordering[Instant].reverse))
        .map(_.slice(offset, offset + limit))
    }

  /**
   * Counts the total number of articles matching the given search parameters.
   *
   * @param params the search parameters to apply
   * @return the total number of articles matching all search criteria
   */
  override def searchCount(params: List[ArticleRepository.SearchParam]): Int < Effect =
    InMemoryTransaction { state =>
      state.articles.get
        .map(articles => filter(articles.values.toList, params))
        .map(_.size)
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
   * Filters articles by a specific tag, reading the tag→articles map stored
   * in [[InMemoryState.tags]] (which is what [[InMemoryTagRepository.add]]
   * writes to). We intentionally do not look at [[Article.tags]] because that
   * field is not maintained by the tag repository.
   */
  private def filter(articles: List[Article], tag: SearchParam.Tag): List[Article] < Effect =
    articles.filter(_.tags.contains(tag.value))

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
        profile <- state.profileByUsername
      } yield profile.get(author.username) match {
        case None                               => Nil
        case Some(UserProfile(userId = userId)) => articles.filter(_.authorId == userId)
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

  /**
   * Finds an article by its slug.
   *
   * @param slug the article slug
   * @return a Maybe containing the article if found, or None if not found
   */
  override def findBySlug(slug: String): Maybe[Article] < Effect =
    InMemoryTransaction { state =>
      for {
        articles <- state.articleBySlug
      } yield Maybe.fromOption(articles.get(slug))

    }

  /**
   * Retrieves a feed of articles for a specific user.
   *
   * The feed consists of articles from authors that the user follows.
   *
   * @param userId the ID of the user for whom to retrieve the feed
   * @param offset the starting index for pagination
   * @param limit  the maximum number of articles to return
   * @return a list of articles in the user's feed
   */
  override def feedOf(userId: Id, offset: Int, limit: Int): List[Article] < Effect =
    InMemoryTransaction { state =>
      for {
        followed <- state.followers.get
        profiles <- state.profiles.get
        articles <- state.articlesByUserId
      } yield {
        val followedProfileIds = followed.getOrElse(userId, Nil)
        val followedUserIds    = followedProfileIds.flatMap(pid => profiles.get(pid).map(_.userId))
        followedUserIds
          .flatMap(authorId => articles.getOrElse(authorId, Nil))
          .sortBy(_.createdAt)(using Ordering[java.time.Instant].reverse)
          .slice(offset, offset + limit)
      }
    }

  /**
   * Counts the total number of articles in a user's feed.
   * 
   * @param userId the ID of the user whose feed articles to count
   * @return the total count of articles in the user's feed
   */
  override def countFeedOf(userId: Id): Int < Effect =
    InMemoryTransaction { state =>
      for {
        followed <- state.followers.get
        profiles <- state.profiles.get
        articles <- state.articlesByUserId
      } yield {
        val followedProfileIds = followed.getOrElse(userId, Nil)
        val followedUserIds    = followedProfileIds.flatMap(pid => profiles.get(pid).map(_.userId))
        followedUserIds.flatMap(authorId => articles.getOrElse(authorId, Nil)).size
      }
    }
}
