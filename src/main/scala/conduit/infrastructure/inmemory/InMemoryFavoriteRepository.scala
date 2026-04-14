package conduit.infrastructure.inmemory

import conduit.domain.model.{ Article, User }
import conduit.domain.service.persistence.FavoriteRepository
import conduit.infrastructure.inmemory.InMemoryState.Changed.{ Deleted, Inserted, Updated }
import conduit.infrastructure.inmemory.InMemoryState.RowReference.{ ArticleRow, FavoriteRow }
import kyo.*

class InMemoryFavoriteRepository extends FavoriteRepository[InMemoryTransaction] {

  /**
   * Checks if a favorite entry exists for an article by a user.
   *
   * @param favorite the favorite entry to check
   * @return true if the favorite entry exists, false otherwise
   */
  override def exists(favorite: Article.Favorite): Boolean < Effect =
    InMemoryTransaction { state =>
      state.favorites.get.map { favorites =>
        favorites.get(favorite.userId).exists(_.contains(favorite.articleId))
      }
    }

  /**
   * Finds favorite entries for a user across multiple articles.
   * 
   * @param userId the ID of the user
   * @param articleIds the list of article IDs to check
   * @return List of favorite entries found
   */
  override def favoriteOf(userId: User.Id, articleIds: List[Article.Id]): List[Article.Id] < Effect =
    InMemoryTransaction { state =>
      state.favorites.get.map: favorites =>
        val favoredIds = favorites.getOrElse(userId, Nil).toSet
        articleIds.filter(favoredIds.contains)
    }

  /**
   * Adds a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to add
   * @return Unit on successful addition
   */
  override def add(favorite: Article.Favorite): Unit < Effect =
    InMemoryTransaction { state =>
      for {
        favorites <- state.favorites.updateAndGet: favorites =>
                       val updatedIds = favorite.articleId :: favorites.getOrElse(favorite.userId, Nil)
                       favorites + (favorite.userId -> updatedIds.distinct)
        _         <- state.addChange:
                       if favorites.get(favorite.userId).exists(_.size == 1)
                       then Inserted(FavoriteRow(favorite.userId))
                       else Updated(FavoriteRow(favorite.userId))
        count      = favorites.values.count(_.contains(favorite.articleId))
        _         <- refreshArticleFavoriteCount(state, favorite.articleId, count)
      } yield ()
    }

  /**
   * Deletes a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to delete
   * @return Unit on successful deletion
   */
  override def delete(favorite: Article.Favorite): Unit < Effect =
    InMemoryTransaction { state =>
      for {
        favorites <- state.favorites.updateAndGet: favorites =>
                       favorites
                         .getOrElse(favorite.userId, Nil)
                         .filterNot(_ == favorite.articleId) match
                         case Nil => favorites - favorite.userId
                         case ids => favorites + (favorite.userId -> ids)
        _         <- state.addChange:
                       if !favorites.contains(favorite.userId)
                       then Deleted(FavoriteRow(favorite.userId))
                       else Updated(FavoriteRow(favorite.userId))
        count      = favorites.values.count(_.contains(favorite.articleId))
        _         <- refreshArticleFavoriteCount(state, favorite.articleId, count)
      } yield ()
    }

  /**
   * Recomputes the favorite count for an article and records the change.
   *
   * @param state the current in-memory state
   * @param count the new favorite count for the article
   * @param articleId the article whose favorite count needs refreshing
   * @return Unit after updating the article and recording the change
   */
  private def refreshArticleFavoriteCount(state: InMemoryState, articleId: Article.Id, count: Int): Unit < kyo.Sync =
    for {
      _ <- state.articles.updateAndGet: articles =>
             articles.get(articleId) match
               case Some(article) => articles + (articleId -> article.copy(favoriteCount = count))
               case None          => articles
      _ <- state.addChange(Updated(ArticleRow(articleId)))
    } yield ()
}
