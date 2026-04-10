package conduit.infrastructure.inmemory

import conduit.domain.model.Article
import conduit.domain.service.persistence.TagRepository
import conduit.infrastructure.inmemory.InMemoryState.Changed.{ Deleted, Inserted, Updated }
import conduit.infrastructure.inmemory.InMemoryState.RowReference.TagsRow
import kyo.*

/**
 * In-memory implementation of the TagRepository for the Conduit application.
 *
 * This repository stores article tags in memory and provides operations for adding,
 * finding, and deleting tags. All operations are wrapped in InMemoryTransaction
 * to ensure consistent access to the shared tags state.
 */
class InMemoryTagRepository extends TagRepository[InMemoryTransaction] {

  /**
   * Retrieves all tags in the system.
   *
   * Returns a complete list of all unique tags that have been added to articles.
   *
   * @return a list of all unique tag strings
   */
  override def findAll: List[String] < Effect =
    InMemoryTransaction { state =>
      state.tags.get.map(_.values.flatten.toList.distinct)
    }

  /**
   * Associates one or more tags with an article.
   *
   * Adds a list of tags to an article, creating tag-article relationships.
   *
   * @param articleId the ID of the article to add tags to
   * @param tags the list of tag strings to associate with the article
   * @return Unit
   */
  override def add(articleId: Article.Id, tags: List[String]): Unit < Effect =
    InMemoryTransaction { state =>
      state
        .tags
        .updateAndGet { current =>
          val existingTags = current.getOrElse(articleId, Nil)
          current.updated(articleId, existingTags ++ tags)
        }
        .flatMap { updatedTags =>
          state.addChange(
            if updatedTags.get(articleId).exists(_.size == tags.size)
            then Inserted(TagsRow(articleId))
            else Updated(TagsRow(articleId))
          )
        }
        .unit
    }

  /**
   * Retrieves all tags associated with a specific article.
   *
   * @param articleId the ID of the article to retrieve tags for
   * @return a list of tag strings associated with the specified article
   */
  override def find(articleId: Article.Id): List[String] < Effect =
    InMemoryTransaction { state =>
      state.tags.get.map(_.getOrElse(articleId, Nil))
    }

  /**
   * Removes all tags associated with an article.
   *
   * Deletes all tag-article relationships for the specified article.
   *
   * @param articleId the ID of the article whose tags should be deleted
   * @return Unit
   */
  override def delete(articleId: Article.Id, tags: List[String]): Unit < Effect =
    InMemoryTransaction { state =>
      state
        .tags
        .updateAndGet { current =>
          val existingTags = current.getOrElse(articleId, Nil)
          val updatedTags  = existingTags.filterNot(tags.contains)
          if updatedTags.isEmpty then current - articleId
          else current.updated(articleId, updatedTags)
        }
        .flatMap { updatedTags =>
          state.addChange(
            if !updatedTags.contains(articleId)
            then Deleted(TagsRow(articleId))
            else Updated(TagsRow(articleId))
          )
        }
        .unit
    }
}
