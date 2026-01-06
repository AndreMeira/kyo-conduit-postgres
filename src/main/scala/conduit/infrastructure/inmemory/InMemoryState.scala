package conduit.infrastructure.inmemory

import conduit.domain.error.ApplicationError
import conduit.domain.model.*
import conduit.infrastructure.inmemory.InMemoryState.LockFailed
import kyo.*

/**
 * In-memory state container for the Conduit application.
 *
 * This class manages all application data including articles, comments, user profiles,
 * favorites, followers, and credentials. It provides thread-safe access through atomic
 * references and includes computed views (indexes) for efficient lookups by various criteria.
 *
 * @param lock a Meter for controlling concurrent access to the state
 * @param articles atomic reference to the map of articles indexed by ID
 * @param comments atomic reference to the map of comments indexed by ID
 * @param profiles atomic reference to the map of user profiles indexed by ID
 * @param favorites atomic reference to the map of article IDs favorited by each user
 * @param followers atomic reference to the map of user profile IDs following each user
 * @param credentials atomic reference to the map of hashed credentials indexed by user ID
 */
class InMemoryState(
  lock: Meter,
  val articles: AtomicRef[Map[Article.Id, Article]],
  val comments: AtomicRef[Map[Long, Comment]],
  val profiles: AtomicRef[Map[UserProfile.Id, UserProfile]],
  val favorites: AtomicRef[Map[User.Id, List[Article.Id]]],
  val followers: AtomicRef[Map[User.Id, List[UserProfile.Id]]],
  val credentials: AtomicRef[Map[User.Id, Credentials.Hashed]],
) {
  
  /**
   * Returns a map of user profiles indexed by their username.
   *
   * @return a map where keys are usernames and values are UserProfile objects
   */
  def profileByUsername: Map[String, UserProfile] < Sync =
    for {
      profiles: List[UserProfile] <- profiles.get.map(_.values.toList)
    } yield profiles.map(profile => profile.name -> profile).toMap

  /**
   * Returns a map of user profiles indexed by their user ID.
   *
   * @return a map where keys are user IDs and values are UserProfile objects
   */
  def profileByUserId: Map[User.Id, UserProfile] < Sync =
    for {
      profiles: List[UserProfile] <- profiles.get.map(_.values.toList)
    } yield profiles.map(profile => profile.userId -> profile).toMap

  /**
   * Returns a map of articles grouped by their author user ID.
   *
   * @return a map where keys are user IDs and values are lists of articles authored by that user
   */
  def articlesByUserId: Map[User.Id, List[Article]] < Sync =
    for {
      articles <- articles.get.map(_.values.toList)
    } yield articles.groupBy(_.authorId)

  /**
   * Returns a map of articles grouped by their author's username.
   *
   * @return a map where keys are usernames and values are lists of articles authored by that user
   */
  def articlesByAuthorName: Map[String, List[Article]] < Sync =
    for {
      articles <- articlesByUserId
      profiles <- profileByUserId
    } yield for {
      (userId, articles) <- articles
      profile            <- profiles.get(userId)
    } yield profile.name -> articles

  /**
   * Returns a map of favorited article IDs grouped by the username of users who favorited them.
   *
   * @return a map where keys are usernames and values are lists of article IDs favorited by that user
   */
  def favoriteByUsername: Map[String, List[Article.Id]] < Sync =
    for {
      profiles  <- profileByUserId
      favorites <- favorites.get
    } yield for {
      (userId, favorites) <- favorites
      profile             <- profiles.get(userId)
    } yield profile.name -> favorites

  /**
   * Returns a map of comments grouped by their article ID.
   *
   * @return a map where keys are article IDs and values are lists of comments for that article
   */
  def commentsByArticleId: Map[Article.Id, List[Comment]] < Sync =
    for {
      comments <- comments.get.map(_.values.toList)
    } yield comments.groupBy(_.articleId)

  /**
   * Creates a deep copy of the current state with new atomic references.
   *
   * This is useful for transaction isolation and rollback scenarios.
   *
   * @return a new InMemoryState instance with duplicated data
   */
  def duplicate: InMemoryState < Sync =
    for {
      articles    <- articles.get.flatMap(AtomicRef.init)
      comments    <- comments.get.flatMap(AtomicRef.init)
      profiles    <- profiles.get.flatMap(AtomicRef.init)
      favorites   <- favorites.get.flatMap(AtomicRef.init)
      followers   <- followers.get.flatMap(AtomicRef.init)
      credentials <- credentials.get.flatMap(AtomicRef.init)
    } yield InMemoryState(lock, articles, comments, profiles, favorites, followers, credentials)

  /**
   * Merges another InMemoryState into the current state.
   *
   * All data from the other state is combined with the current state using a lock
   * to ensure atomicity. Uses the lock-acquire pattern to prevent concurrent modifications.
   *
   * @param other the InMemoryState to merge into this state
   * @return Unit on successful merge
   * @throws LockFailed if the lock cannot be acquired
   */
  def merge(other: InMemoryState): Unit < (Async & Abort[LockFailed.type]) =
    lock
      .run {
        for {
          _ <- other.articles.get.flatMap(other => articles.updateAndGet(_ ++ other))
          _ <- other.comments.get.flatMap(other => comments.updateAndGet(_ ++ other))
          _ <- other.profiles.get.flatMap(other => profiles.updateAndGet(_ ++ other))
          _ <- other.favorites.get.flatMap(other => favorites.updateAndGet(_ ++ other))
          _ <- other.followers.get.flatMap(other => followers.updateAndGet(_ ++ other))
          _ <- other.credentials.get.flatMap(other => credentials.updateAndGet(_ ++ other))
        } yield ()
      }
      .mapAbort(_ => InMemoryState.LockFailed)
}

object InMemoryState:
  /**
   * Error thrown when a lock cannot be acquired during state operations.
   */
  object LockFailed extends ApplicationError.TransientError:
    override def message: String = "Failed to acquire lock"
