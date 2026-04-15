package conduit.domain.request.user

import conduit.domain.model.{ User, UserProfile }
import conduit.domain.request.Patchable
import conduit.domain.types.*
import kyo.Maybe

import java.net.URI

/**
 * Represents a request to update the details of the authenticated user in the Conduit application.
 *
 * This request is made by an authenticated user who wants to modify their own user details,
 * such as email, username, password, bio, or image.
 *
 * @param requester the authenticated user making the request
 * @param payload the data containing the fields to be updated
 */
case class UpdateUserRequest(
  requester: User.Authenticated,
  payload: UpdateUserRequest.Payload,
)

object UpdateUserRequest:

  /**
   * Represents the payload for updating a user's details.
   *
   * @param user the data containing the fields to be updated
   */
  case class Payload(user: Data) // wrapping due to api spec

  /**
   * Represents the data for updating a user's details.
   *
   * Each field is optional, allowing partial updates to the user's profile.
   *
   * @param email the new email of the user (optional)
   * @param username the new username of the user (optional)
   * @param password the new password of the user (optional)
   * @param bio the new bio of the user (optional)
   * @param image the new image URL of the user (optional)
   */
  case class Data(
    email: Patchable[String],
    username: Patchable[String],
    password: Patchable[String],
    bio: Patchable[String],
    image: Patchable[String],
  )

  enum Patch:
    case Email(email: conduit.domain.types.Email)
    case Username(username: ProfileName)
    case Password(password: conduit.domain.types.Password)
    case Bio(bio: Maybe[ProfileBiography])
    case Image(image: Maybe[ProfileImage])
