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
  override def exists(favorite: Article.FavoriteBy): Boolean < Effect =
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
      state.favorites.get.map { favorites =>
        val favoredIds = favorites.getOrElse(userId, Nil).toSet
        articleIds.filter(favoredIds.contains)
      }
    }

  /**
   * Adds a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to add
   * @return Unit on successful addition
   */
  override def add(favorite: Article.FavoriteBy): Unit < Effect =
    InMemoryTransaction { state =>
      for {
        favorites <- state
          .favorites
          .updateAndGet { favorites =>
            val updatedIds = favorites.getOrElse(favorite.userId, Nil) :+ favorite.articleId
            favorites + (favorite.userId -> updatedIds.distinct)
          }
        _ <- state.addChange(
          if favorites.get(favorite.userId).exists(_.size == 1)
          then Inserted(FavoriteRow(favorite.userId))
          else Updated(FavoriteRow(favorite.userId))
        )
        count = favorites.values.count(_.contains(favorite.articleId))
        _ <- state.articles.updateAndGet { articles =>
          articles.get(favorite.articleId) match
            case Some(article) => articles + (favorite.articleId -> article.copy(favoriteCount = count))
            case None          => articles
        }
        _ <- state.addChange(Updated(ArticleRow(favorite.articleId)))
      } yield ()
    }

  /**
   * Deletes a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to delete
   * @return Unit on successful deletion
   */
  override def delete(favorite: Article.FavoriteBy): Unit < Effect =
    InMemoryTransaction { state =>
      for {
        favorites <- state
          .favorites
          .updateAndGet { favorites =>
            favorites.getOrElse(favorite.userId, Nil).filterNot(_ == favorite.articleId) match
              case Nil => favorites - favorite.userId
              case ids => favorites + (favorite.userId -> ids)
          }
        _ <- state.addChange(
          if !favorites.contains(favorite.userId)
          then Deleted(FavoriteRow(favorite.userId))
          else Updated(FavoriteRow(favorite.userId))
        )
        count = favorites.values.count(_.contains(favorite.articleId))
        _ <- state.articles.updateAndGet { articles =>
          articles.get(favorite.articleId) match
            case Some(article) => articles + (favorite.articleId -> article.copy(favoriteCount = count))
            case None          => articles
        }
        _ <- state.addChange(Updated(ArticleRow(favorite.articleId)))
      } yield ()
    }
}
