package conduit.domain.request.user

import conduit.domain.model.{User, UserProfile}
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
    email: Option[String],
    username: Option[String],
    password: Option[String],
    bio: Option[String],
    image: Option[String],
  )
  
  enum Patch:
    case Email(email: String)
    case Username(username: String)
    case Password(password: String)
    case Bio(bio: Maybe[String])
    case Image(image: Maybe[URI])