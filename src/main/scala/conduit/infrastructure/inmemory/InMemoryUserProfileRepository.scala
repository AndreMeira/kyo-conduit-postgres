package conduit.infrastructure.inmemory

import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.service.persistence.UserProfileRepository
import conduit.infrastructure.inmemory.InMemoryState.Changed.{ Inserted, Updated }
import conduit.infrastructure.inmemory.InMemoryState.RowReference.ProfileRow
import kyo.*

/**
 * In-memory implementation of the UserProfileRepository for the Conduit application.
 *
 * This repository stores user profiles in memory and provides operations for finding, saving,
 * updating, and checking existence of user profiles. All operations are wrapped in InMemoryTransaction
 * to ensure consistent access to the shared user profile state.
 */
class InMemoryUserProfileRepository extends UserProfileRepository[InMemoryTransaction] {

  /**
   * Saves a new user profile to the repository.
   *
   * @param profile the user profile to save
   * @return Unit
   */
  override def save(profile: UserProfile): Unit < Effect =
    InMemoryTransaction { state =>
      state.profiles.updateAndGet(_ + (profile.id -> profile))
        *> state.addChange(Inserted(ProfileRow(profile.id)))
    }

  /**
   * Updates an existing user profile in the repository.
   *
   * @param profile the user profile with updated data
   * @return Unit
   */
  override def update(profile: UserProfile): Unit < Effect =
    InMemoryTransaction { state =>
      state.profiles.updateAndGet(_ + (profile.id -> profile))
        *> state.addChange(Updated(ProfileRow(profile.id)))
    }

  /**
   * Checks if a user profile with the given ID exists in the repository.
   *
   * @param id the user profile ID to check
   * @return true if the user profile exists, false otherwise
   */
  override def exists(id: UserProfile.Id): Boolean < Effect =
    InMemoryTransaction { state =>
      state.profiles.get.map(_.contains(id))
    }

  /**
   * Finds a user profile by its unique ID.
   *
   * @param id the user profile ID to search for
   * @return a Maybe containing the user profile if found, or None if not found
   */
  override def find(id: UserProfile.Id): Maybe[UserProfile] < Effect =
    InMemoryTransaction { state =>
      state.profiles.get.map(_.get(id)).map(Maybe.fromOption)
    }

  /**
   * Finds a user profile by the associated user ID.
   *
   * @param id the user ID to search for
   * @return a Maybe containing the user profile if found, or None otherwise
   */
  override def findByUser(id: User.Id): Maybe[UserProfile] < Effect =
    InMemoryTransaction { state =>
      for {
        profileByUserId <- state.profileByUserId
      } yield Maybe.fromOption(profileByUserId.get(id))
    }

  /**
   * Finds user profiles for a set of user IDs.
   * 
   * @param ids the list of user IDs to search for
   * @return a list of user profiles corresponding to the provided user IDs
   */
  override def findByUsers(ids: List[User.Id]): List[UserProfile] < Effect =
    InMemoryTransaction { state =>
      for {
        profileByUserId <- state.profileByUserId
      } yield ids.flatMap(id => profileByUserId.get(id))
    }

  /**
   * Finds a user profile by the associated article ID.
   *
   * @param id the article ID to search for
   * @return a Maybe containing the user profile if found, or None otherwise
   */
  override def findByArticle(id: Article.Id): Maybe[UserProfile] < Effect =
    InMemoryTransaction { state =>
      for {
        articles        <- state.articles.get
        profileByUserId <- state.profileByUserId
      } yield for {
        article <- Maybe.fromOption(articles.get(id))
        profile <- Maybe.fromOption(profileByUserId.get(article.authorId))
      } yield profile
    }

  /**
   * Finds user profiles for a set of article IDs.
   *
   * @param ids the set of article IDs to search for
   *  @return a map of article IDs to their corresponding user profiles
   */
  override def findByArticles(ids: Set[Article.Id]): Map[Article.Id, UserProfile] < Effect =
    InMemoryTransaction { state =>
      for {
        articles        <- state.articles.get
        profileByUserId <- state.profileByUserId
      } yield {
        for {
          articleId <- ids
          article   <- articles.get(articleId)
          profile   <- profileByUserId.get(article.authorId)
        } yield articleId -> profile
      }.toMap
    }

  /**
   * Tells if the username exists
   *
   * @param username the username to search for
   * @return true if the profile exists false otherwise
   */
  override def exists(username: String): Boolean < Effect =
    InMemoryTransaction { state =>
      state.profiles.get.map(_.values.exists(_.name == username))
    }

  /**
   * Finds a user profile by the username.
   *
   * @param username the username to search for
   * @return a Maybe containing the user profile if found, or None otherwise
   */
  override def findByUsername(username: String): Maybe[UserProfile] < Effect =
    InMemoryTransaction { state =>
      state.profiles.get.map { profiles =>
        Maybe.fromOption(profiles.values.find(_.name == username))
      }
    }
}
