package conduit.domain.model

import kyo.Maybe

import java.net.URI
import java.time.Instant
import java.util.UUID

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
  type Id = UUID

  case class FollowedBy(
    followerId: User.Id,
    profileId: UserProfile.Id,
  )
