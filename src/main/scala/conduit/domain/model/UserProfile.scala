package conduit.domain.model

import kyo.Maybe

import java.net.URI
import java.time.Instant
import java.util.UUID

/**
 * Represents a user's public profile in the Conduit application.
 *
 * User profiles contain public information that other users can view, including
 * biographical information and profile images. Profiles are separate from user
 * credentials to maintain a clear separation between authentication data and
 * public profile information.
 *
 * @param id the unique identifier for the user profile
 * @param userId the unique identifier of the user this profile belongs to
 * @param name the display name or username shown to other users
 * @param biography an optional biographical description provided by the user
 * @param image an optional profile image URI
 * @param createdAt the timestamp when the profile was created
 * @param updatedAt the timestamp when the profile was last modified
 */
case class UserProfile(
  id: UserProfile.Id,
  userId: User.Id,
  name: String,
  biography: Maybe[String],
  image: Maybe[URI],
  createdAt: Instant,
  updatedAt: Instant,
)

object UserProfile:
  /** Type alias for user profile identifiers using UUID */
  type Id = UUID

  /**
   * Represents a follower relationship between users.
   *
   * This is used to track which users are following which user profiles,
   * enabling features like following/unfollowing users and displaying
   * follower counts and lists.
   *
   * @param followerId the unique identifier of the user doing the following
   * @param profileId the unique identifier of the profile being followed
   */
  case class Follower(
    followerId: User.Id,
    profileId: UserProfile.Id,
  )
