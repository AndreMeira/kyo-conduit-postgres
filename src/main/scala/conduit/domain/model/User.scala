package conduit.domain.model

import conduit.domain.syntax.*
import kyo.Maybe

import java.util.UUID

/**
 * Represents the authentication state of a user in the Conduit application.
 *
 * Users can either be anonymous (not logged in) or authenticated with a valid user ID.
 * This enum is used throughout the application to handle authentication requirements
 * and enforce access control for protected operations.
 */
enum User:
  /** Represents an unauthenticated user with no access to protected resources */
  case Anonymous

  /**
   * Represents an authenticated user with a valid user ID
   *
   * @param userId the unique identifier for the authenticated user
   */
  case Authenticated(userId: User.Id)

  /**
   * Extract the user id if authenticated
   * 
   * @return the Present(user id) if authenticated, Emtpy otherwise
   */
  def option: Maybe[User.Id] = this match {
    case Anonymous         => Maybe.Absent
    case Authenticated(id) => Maybe.Present(id)
  }

object User:
  /** Type alias for user identifiers using UUID */
  type Id = UUID

  /**
   * Represents a signed authentication token for a user.
   *
   * @param value the string representation of the signed token
   */
  case class SignedToken(value: String)
