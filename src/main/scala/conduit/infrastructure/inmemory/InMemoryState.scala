package conduit.infrastructure.inmemory

import conduit.domain.error.ApplicationError
import conduit.domain.model.*
import conduit.infrastructure.inmemory.InMemoryState.Changed.*
import conduit.infrastructure.inmemory.InMemoryState.RowReference.*
import conduit.infrastructure.inmemory.InMemoryState.{ Changed, RowReference }
import kyo.*

/**
 * In-memory state container for the Conduit application.
 *
 * This class manages all application data including articles, comments, user profiles,
 * favorites, followers, and credentials. It provides thread-safe access through atomic
 * references and includes computed views (indexes) for efficient lookups by various criteria.
 *
 * Changes to the state are tracked to enable conflict detection during transaction merges.
 *
 * @param lock a Meter for controlling concurrent access to the state
 * @param articles atomic reference to the map of articles indexed by ID
 * @param comments atomic reference to the map of comments indexed by ID
 * @param profiles atomic reference to the map of user profiles indexed by ID
 * @param favorites atomic reference to the map of article IDs favorited by each user
 * @param followers atomic reference to the map of user profile IDs following each user
 * @param credentials atomic reference to the map of hashed credentials indexed by user ID
 * @param tags atomic reference to the map of tags associated with each article ID
 * @param changes atomic reference to the list of changes made to the state
 */
class InMemoryState(
  lock: Meter,
  val articles: AtomicRef[Map[Article.Id, Article]],
  val comments: AtomicRef[Map[Comment.Id, Comment]],
  val profiles: AtomicRef[Map[UserProfile.Id, UserProfile]],
  val favorites: AtomicRef[Map[User.Id, List[Article.Id]]],
  val followers: AtomicRef[Map[User.Id, List[UserProfile.Id]]],
  val credentials: AtomicRef[Map[User.Id, Credentials.Hashed]],
  val tags: AtomicRef[Map[Article.Id, List[String]]],
  val changes: AtomicRef[List[InMemoryState.Changed]],
) {

  /**
   * Returns a map of user profiles indexed by their username.
   *
   * @return a map where keys are usernames and values are UserProfile objects
   */
  def profileByUsername: Map[String, UserProfile] < Sync =
    for profiles: List[UserProfile] <- profiles.get.map(_.values.toList)
    yield profiles.map(profile => profile.name -> profile).toMap

  /**
   * Returns a map of user profiles indexed by their user ID.
   *
   * @return a map where keys are user IDs and values are UserProfile objects
   */
  def profileByUserId: Map[User.Id, UserProfile] < Sync =
    for profiles: List[UserProfile] <- profiles.get.map(_.values.toList)
    yield profiles.map(profile => profile.userId -> profile).toMap

  /**
   * Returns a map of articles grouped by their author user ID.
   *
   * @return a map where keys are user IDs and values are lists of articles authored by that user
   */
  def articlesByUserId: Map[User.Id, List[Article]] < Sync =
    for articles <- articles.get.map(_.values.toList)
    yield articles.groupBy(_.authorId)

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
    for comments <- comments.get.map(_.values.toList)
    yield comments.groupBy(_.articleId)

  /**
   * Returns a map where keys are slugs as string and values are the matching Article
   *
   * @return a map where keys are article IDs and values are lists of comments for that article
   */
  def articleBySlug: Map[String, Article] < Sync =
    for articles <- articles.get.map(_.values)
    yield articles.map(article => article.slug -> article).toMap

  /**
   * Adds a change record to the list of changes.
   *
   * @param change the change to be recorded
   * @return Unit on successful addition
   */
  def addChange(change: Changed): Unit < Sync =
    changes.updateAndGet(_ :+ change).unit

  /**
   * Retrieves a list of all deleted row references from the change log.
   *
   * @return a list of RowReference objects representing deleted rows
   */
  def deleted: List[RowReference] < Sync =
    for changes <- changes.get
    yield changes.collect { case Deleted(row) => row }

  /**
   * Retrieves a list of all updated row references from the change log.
   *
   * @return a list of RowReference objects representing updated rows
   */
  def updated: List[RowReference] < Sync =
    for changes <- changes.get
    yield changes.collect { case Updated(row) => row }

  /**
   * Identifies "phantom" updates in another state that reference rows deleted in the current state.
   *
   * This method checks for any updated rows in the other state that correspond to rows deleted
   * in the current state. It returns a list of RowReference objects for any such phantom updates,
   * which can be used to prevent conflicts during merges.
   *
   * @param other the other InMemoryState to compare against
   * @return a list of RowReference objects representing phantom updates
   */
  private def phantomUpdate(other: InMemoryState): List[RowReference] < Sync =
    for {
      updated     <- other.updated
      articles    <- articles.get.map(_.keys.toSet)
      comments    <- comments.get.map(_.keys.toSet)
      profiles    <- profiles.get.map(_.keys.toSet)
      favorites   <- favorites.get.map(_.keys.toSet)
      followers   <- followers.get.map(_.keys.toSet)
      credentials <- credentials.get.map(_.keys.toSet)
      tags        <- tags.get.map(_.keys.toSet)
    } yield updated.filter {
      case ArticleRow(id)         => !articles.contains(id)
      case CommentRow(id)         => !comments.contains(id)
      case ProfileRow(id)         => !profiles.contains(id)
      case FavoriteRow(userId)    => !favorites.contains(userId)
      case FollowerRow(userId)    => !followers.contains(userId)
      case CredentialsRow(userId) => !credentials.contains(userId)
      case TagsRow(articleId)     => !tags.contains(articleId)
    }

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
      tags        <- tags.get.flatMap(AtomicRef.init)
      changes     <- AtomicRef.init(List.empty[InMemoryState.Changed])
    } yield InMemoryState(lock, articles, comments, profiles, favorites, followers, credentials, tags, changes)

  /**
   * Prepares another state for merging by removing deleted rows from it.
   *
   * This method removes all rows from the other state that have been marked as deleted
   * in the current state's change log. This ensures that deleted rows in the current
   * state won't reappear during the merge operation.
   *
   * @param other the state to be prepared for merging
   * @return Unit after removing all deleted rows from the other state
   */
  private def prepareMerge(other: InMemoryState): Unit < Sync =
    for {
      phantom <- phantomUpdate(other)
      _       <- other.articles.updateAndGet(_ -- phantom.collect { case ArticleRow(id) => id })
      _       <- other.comments.updateAndGet(_ -- phantom.collect { case CommentRow(id) => id })
      _       <- other.profiles.updateAndGet(_ -- phantom.collect { case ProfileRow(id) => id })
      _       <- other.favorites.updateAndGet(_ -- phantom.collect { case FavoriteRow(id) => id })
      _       <- other.followers.updateAndGet(_ -- phantom.collect { case FollowerRow(id) => id })
      _       <- other.credentials.updateAndGet(_ -- phantom.collect { case CredentialsRow(id) => id })
      _       <- other.tags.updateAndGet(_ -- phantom.collect { case TagsRow(id) => id })
    } yield ()

  /**
   * Verify that we respect unicity on email
   *
   * @param other another state to be merged
   * @return Unit and fails if constraint is violated
   */
  private def checkEmailUnicity(other: InMemoryState): Unit < (Sync & Abort[InMemoryState.Failure]) =
    for {
      existing    <- credentials.get
      created     <- other.credentials.get
      diff         = created -- existing.keySet
      intersection = diff.values.map(_.email).toSet & existing.values.map(_.email).toSet
      _           <- if intersection.isEmpty then Kyo.unit
                     else Abort.fail(InMemoryState.Failure.ConstraintViolation)
    } yield ()

  /**
   * Verify that we respect unicity on username
   *
   * @param other another state to be merged
   * @return Unit and fails if constraint is violated
   */
  private def checkUsernameUnicity(other: InMemoryState): Unit < (Sync & Abort[InMemoryState.Failure]) =
    for {
      existing    <- profiles.get
      created     <- other.profiles.get
      diff         = created -- existing.keySet
      intersection = diff.values.map(_.name).toSet & existing.values.map(_.name).toSet
      _           <- if intersection.isEmpty then Kyo.unit
                     else Abort.fail(InMemoryState.Failure.ConstraintViolation)
    } yield ()

  /**
   * Performs all pre-merge validation checks before merging another state.
   *
   * This method executes a series of checks to ensure the merge can proceed safely:
   * 1. Prepares the other state by removing deleted rows
   * 2. Checks for consistency conflicts (deleted vs updated rows)
   * 3. Verifies email uniqueness constraints
   * 4. Verifies username uniqueness constraints
   *
   * @param other the state to be validated and prepared for merging
   * @return Unit if all validations pass
   * @throws LockFailed if the lock cannot be acquired
   * @throws ConstraintViolation if any validation check fails
   */
  private def beforeMerge(other: InMemoryState): Unit < (Async & Abort[InMemoryState.Failure]) =
    prepareMerge(other)
      *> checkEmailUnicity(other)
      *> checkUsernameUnicity(other)

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
  def merge(other: InMemoryState): Unit < (Async & Abort[InMemoryState.Failure]) =
    lock
      .run {
        beforeMerge(other)
          *> removeDeleted(other)
          *> mergeData(other)
          *> mergeChanges(other)
      }
      .mapAbort(err => InMemoryState.Failure.LockFailed(err.toString))
      .unit

  /**
   * Removes rows from the current state that were deleted in the other state.
   *
   * @param other the state whose deletions should be applied to the current state
   * @return Unit after removing all deleted rows
   */
  private def removeDeleted(other: InMemoryState): Unit < Sync =
    other.deleted.flatMap { deleted =>
      articles.updateAndGet(_ -- deleted.collect { case ArticleRow(id) => id })
        *> comments.updateAndGet(_ -- deleted.collect { case CommentRow(id) => id })
        *> profiles.updateAndGet(_ -- deleted.collect { case ProfileRow(id) => id })
        *> favorites.updateAndGet(_ -- deleted.collect { case FavoriteRow(id) => id })
        *> followers.updateAndGet(_ -- deleted.collect { case FollowerRow(id) => id })
        *> credentials.updateAndGet(_ -- deleted.collect { case CredentialsRow(id) => id })
        *> tags.updateAndGet(_ -- deleted.collect { case TagsRow(id) => id })
    }.unit

  /**
   * Merges all data from the other state into the current state.
   *
   * @param other the state whose data should be merged into the current state
   * @return Unit after merging all data
   */
  private def mergeData(other: InMemoryState): Unit < Sync =
    other.articles.get.flatMap(other => articles.updateAndGet(_ ++ other))
      *> other.comments.get.flatMap(other => comments.updateAndGet(_ ++ other))
      *> other.profiles.get.flatMap(other => profiles.updateAndGet(_ ++ other))
      *> other.favorites.get.flatMap(other => favorites.updateAndGet(_ ++ other))
      *> other.followers.get.flatMap(other => followers.updateAndGet(_ ++ other))
      *> other.credentials.get.flatMap(other => credentials.updateAndGet(_ ++ other))
      *> other.tags.get.flatMap(other => tags.updateAndGet(_ ++ other)).unit

  /**
   * Merges changes from the other state, keeping only the last 10k changes to prevent unbounded growth.
   *
   * @param other the state whose changes should be merged into the current state
   * @return Unit after merging changes
   */
  private def mergeChanges(other: InMemoryState): Unit < Sync =
    other.changes.get.flatMap(other => changes.updateAndGet(_ ++ other))
      *> changes.get.flatMap(changes => changes.drop(changes.size - 10000)).unit

  /**
   * Clears all data from the state, resetting it to an empty state.
   *
   * This method is useful for testing purposes to ensure a clean slate before each test case.
   *
   * @return Unit after clearing all data
   */
  def clean: Unit < Sync =
    for
      _ <- articles.updateAndGet(_ => Map.empty)
      _ <- comments.updateAndGet(_ => Map.empty)
      _ <- profiles.updateAndGet(_ => Map.empty)
      _ <- favorites.updateAndGet(_ => Map.empty)
      _ <- followers.updateAndGet(_ => Map.empty)
      _ <- credentials.updateAndGet(_ => Map.empty)
      _ <- tags.updateAndGet(_ => Map.empty)
      _ <- changes.updateAndGet(_ => Nil)
    yield ()
}

object InMemoryState:

  /**
   * Creates an empty InMemoryState with initialized atomic references and a lock.
   *
   * @return a new InMemoryState instance with no data
   */
  def empty: InMemoryState < (Sync & Scope) =
    for
      lock        <- Meter.initMutex
      articles    <- AtomicRef.init(Map.empty[Article.Id, Article])
      comments    <- AtomicRef.init(Map.empty[Comment.Id, Comment])
      profiles    <- AtomicRef.init(Map.empty[UserProfile.Id, UserProfile])
      favorites   <- AtomicRef.init(Map.empty[User.Id, List[Article.Id]])
      followers   <- AtomicRef.init(Map.empty[User.Id, List[UserProfile.Id]])
      credentials <- AtomicRef.init(Map.empty[User.Id, Credentials.Hashed])
      tags        <- AtomicRef.init(Map.empty[Article.Id, List[String]])
      changes     <- AtomicRef.init(List.empty[InMemoryState.Changed])
    yield InMemoryState(lock, articles, comments, profiles, favorites, followers, credentials, tags, changes)

  /**
   * Companion object for InMemoryState providing reference types and change tracking.
   */
  enum RowReference:
    /**
     * Represents a reference to a row in one of the in-memory tables.
     *
     * Used for tracking changes (inserts, updates, deletes) and detecting conflicts
     * during transaction merges.
     */
    /** Reference to an article row identified by article ID. */
    case ArticleRow(articleId: Article.Id)

    /** Reference to a comment row identified by comment ID. */
    case CommentRow(commentId: Comment.Id)

    /** Reference to a user profile row identified by user profile ID. */
    case ProfileRow(id: UserProfile.Id)

    /** Reference to a favorite relationship identified by user ID. */
    case FavoriteRow(userId: User.Id)

    /** Reference to a follower relationship identified by user ID. */
    case FollowerRow(userId: User.Id)

    /** Reference to credentials identified by user ID. */
    case CredentialsRow(userId: User.Id)

    /** Reference to tags associated with an article identified by article ID. */
    case TagsRow(articleId: Article.Id)

  /**
   * Represents a change operation performed on a row.
   *
   * Used for tracking transaction modifications and detecting conflicts
   * such as updating deleted rows or deleting updated rows.
   */
  enum Changed:
    /** A new row was inserted. */
    case Inserted(changed: RowReference)

    /** An existing row was updated. */
    case Updated(changed: RowReference)

    /** An existing row was deleted. */
    case Deleted(changed: RowReference)

  /**
   * Enumeration of possible failures during state operations.
   */
  enum Failure extends ApplicationError.TransientError:

    /** Represents a failure to acquire the lock for state modification. */
    case LockFailed(error: String)

    /** Represents a violation of data constraints during state operations. */
    case ConstraintViolation

    /**
     * Provides a human-readable error message for each failure type.
     *
     * @return the error message corresponding to this failure case
     */
    override def message: String = this match
      case LockFailed(msg)     => s"Failed to acquire lock: $msg"
      case ConstraintViolation => "Constraint violation detected during state operation"
