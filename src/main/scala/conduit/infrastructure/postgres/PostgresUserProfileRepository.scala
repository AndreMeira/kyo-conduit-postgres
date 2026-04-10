package conduit.infrastructure.postgres

import com.augustnagro.magnum.*
import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.service.persistence.UserProfileRepository
import conduit.infrastructure.codecs.database.DatabaseCodecs.given
import conduit.infrastructure.postgres.PostgresTransaction.Transactional
import kyo.*

import java.util.UUID

class PostgresUserProfileRepository extends UserProfileRepository[PostgresTransaction]:

  /**
   * Saves a new user profile to the repository.
   *
   * @param profile the user profile to save
   * @return Unit on successful save
   */
  override def save(profile: UserProfile): Unit < Effect =
    Transactional:
      val count = sql"""
          INSERT INTO profiles (id, user_id, name, bio, image, created_at, updated_at)
          VALUES (
            ${profile.id},
            ${profile.userId},
            ${profile.name},
            ${profile.biography},
            ${profile.image},
            ${profile.createdAt},
            ${profile.updatedAt}
          )"""
        .update
        .run()
      require(count == 1, "Failed to insert user profile")

  /**
   * Updates an existing user profile in the repository.
   *
   * @param profile the user profile with updated data
   * @return Unit on successful update
   */
  override def update(profile: UserProfile): Unit < Effect =
    Transactional:
      val count = sql"""
          UPDATE profiles SET
            name       = ${profile.name},
            bio        = ${profile.biography},
            image      = ${profile.image},
            updated_at = ${profile.updatedAt}
          WHERE id = ${profile.id}"""
        .update
        .run()
      require(count == 1, "Failed to update user profile")

  /**
   * Checks if a user profile with the given ID exists in the repository.
   *
   * @param id the user profile ID to check
   * @return true if the user profile exists, false otherwise
   */
  override def exists(id: UserProfile.Id): Boolean < Effect =
    Transactional:
      sql"""SELECT EXISTS(SELECT 1 FROM profiles WHERE id = $id)"""
        .query[Boolean]
        .run()
        .headOption.contains(true)

  /**
   * Finds a user profile by its unique ID.
   *
   * @param id the user profile ID to search for
   * @return a Maybe containing the user profile if found, or None if not found
   */
  override def find(id: UserProfile.Id): Maybe[UserProfile] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT p.id, p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at
              FROM profiles p
              WHERE p.id = $id"""
          .query[UserProfile]
          .run()
          .headOption

  /**
   * Finds a user profile by the associated user ID.
   *
   * @param id the user ID to search for
   * @return a Maybe containing the user profile if found, or None otherwise
   */
  override def findByUser(id: User.Id): Maybe[UserProfile] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT p.id, p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at
              FROM profiles p
              WHERE p.user_id = $id"""
          .query[UserProfile]
          .run()
          .headOption

  /**
   * Finds user profiles for a list of user IDs.
   *
   * @param ids the list of user IDs to search for
   * @return a list of user profiles corresponding to the provided user IDs
   */
  override def findByUsers(ids: List[User.Id]): List[UserProfile] < Effect =
    if ids.isEmpty then List.empty
    else
      Transactional:
        sql"""SELECT p.id, p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at
              FROM profiles p
              WHERE p.user_id = ANY($ids)"""
          .query[UserProfile]
          .run()
          .toList

  /**
   * Finds a user profile by the username.
   *
   * @param username the username to search for
   * @return a Maybe containing the user profile if found, or None otherwise
   */
  override def findByUsername(username: String): Maybe[UserProfile] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT p.id, p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at
              FROM profiles p
              WHERE p.name = $username"""
          .query[UserProfile]
          .run()
          .headOption

  /**
   * Tells if the username exists.
   *
   * @param username the username to search for
   * @return true if the profile exists, false otherwise
   */
  override def exists(username: String): Boolean < Effect =
    Transactional:
      sql"""SELECT EXISTS(SELECT 1 FROM profiles WHERE name = $username)"""
        .query[Boolean]
        .run()
        .headOption.contains(true)

  /**
   * Finds a user profile by the associated article ID.
   *
   * @param id the article ID to search for
   * @return a Maybe containing the user profile if found, or None otherwise
   */
  override def findByArticle(id: Article.Id): Maybe[UserProfile] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT p.id, p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at
              FROM profiles p
              JOIN articles a ON a.author_id = p.user_id
              WHERE a.id = $id"""
          .query[UserProfile]
          .run()
          .headOption

  /**
   * Finds user profiles for a set of article IDs, returning a map of article ID to profile.
   *
   * @param ids the set of article IDs to search for
   * @return a map of article IDs to their corresponding user profiles
   */
  override def findByArticles(ids: Set[Article.Id]): Map[Article.Id, UserProfile] < Effect =
    if ids.isEmpty then Map.empty
    else
      Transactional:
        val idsList: List[UUID] = ids.toList
        sql"""SELECT a.id, p.id, p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at
              FROM profiles p
              JOIN articles a ON a.author_id = p.user_id
              WHERE a.id = ANY($idsList)"""
          .query[(Article.Id, UserProfile)]
          .run()
          .toMap
