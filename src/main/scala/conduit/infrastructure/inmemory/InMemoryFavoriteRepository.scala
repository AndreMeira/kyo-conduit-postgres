package conduit.infrastructure.inmemory

import conduit.domain.model.Article
import conduit.domain.service.persistence.FavoriteRepository
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
   * Adds a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to add
   * @return Unit on successful addition
   */
  override def add(favorite: Article.FavoriteBy): Unit < Effect =
    InMemoryTransaction { state =>
      state.favorites.updateAndGet { favorites =>
        val updatedIds = favorites.getOrElse(favorite.userId, Nil) :+ favorite.articleId
        favorites + (favorite.userId -> updatedIds.distinct)
      }
    }.unit

  /**
   * Deletes a favorite entry for an article by a user.
   *
   * @param favorite the favorite entry to delete
   * @return Unit on successful deletion
   */
  override def delete(favorite: Article.FavoriteBy): Unit < Effect =
    InMemoryTransaction { state =>
      state.favorites.updateAndGet { favorites =>
        favorites.getOrElse(favorite.userId, Nil).filterNot(_ == favorite.articleId) match
          case Nil => favorites - favorite.userId
          case ids => favorites + (favorite.userId -> ids)
      }
    }.unit
}
