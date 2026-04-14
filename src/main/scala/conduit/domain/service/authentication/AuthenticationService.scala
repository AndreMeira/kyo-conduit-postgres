package conduit.domain.service.authentication

import conduit.domain.error.Unauthorised
import conduit.domain.model.{ Credentials, User }
import conduit.domain.service.authentication.AuthenticationService.Config
import kyo.*
import org.apache.commons.codec.digest.DigestUtils
import pdi.jwt.{ JwtAlgorithm, JwtCirce, JwtClaim }

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps

/**
 * Service for handling authentication-related operations such as password hashing,
 * token encoding/decoding, and user identification from tokens.
 *
 * @param clock  Provides the current time for token expiration and validation.
 * @param config Configuration for password and token handling.
 */
class AuthenticationService(clock: Clock, config: Config) {

  /**
   * Returns a hashed version of the provided credentials.
   * If the credentials are already hashed, returns them as is.
   *
   * @param credentials The credentials to hash.
   * @return The hashed credentials.
   */
  def hashed(credentials: Credentials): Credentials.Hashed < Any =
    credentials match {
      case creds: Credentials.Hashed => creds
      case creds: Credentials.Clear  => hash(creds)
    }

  /**
   * Hashes clear credentials using the configured password salt.
   *
   * @param credentials The clear credentials to hash.
   * @return The hashed credentials.
   */
  def hash(credentials: Credentials.Clear): Credentials.Hashed < Any =
    hashPassword(credentials.password)
      .map(hashedPassword => Credentials.Hashed(credentials.email, hashedPassword))

  /**
   * Hashes a password string using SHA-256 and the configured password salt.
   *
   * @param password The password to hash.
   * @return The hashed password string.
   */
  def hashPassword(password: String): String < Any =
    DigestUtils.sha256Hex(s"$password.${config.passwordSalt}")

  /**  
   * Authenticates a user based on a signed token, returning the authenticated user ID.
   *
   * @param token The signed user token.
   * @return The authenticated user ID if the token is valid.
   * @throws Unauthorised if the token is invalid or expired.
   */
  def authenticate(token: User.SignedToken): User.Authenticated < (Sync & Abort[Unauthorised]) =
    user(token).map(User.Authenticated(_))

  /**
   * Authenticates a user based on an optional signed token, returning either an authenticated user ID or anonymous.
   * 
   * @param token The optional signed user token.
   * @return The authenticated user ID if the token is valid, or anonymous if the token is absent.
   */
  def authenticate(token: Maybe[User.SignedToken]): User < (Sync & Abort[Unauthorised]) =
    user(token).map {
      case Maybe.Present(userId) => User.Authenticated(userId).recover(_ => User.Anonymous)
      case Maybe.Absent          => User.Anonymous
    }

  /**
   * Extracts the user ID from a signed token, verifying its validity and expiration.
   *
   * @param token The signed user token.
   * @return The user ID if the token is valid.
   * @throws Unauthorised if the token is invalid or expired.
   */
  def user(token: User.SignedToken): User.Id < (Sync & Abort[Unauthorised]) =
    for {
      claim  <- decodeToken(token)
      _      <- verifyExpiration(claim)
      userId <- parseUserId(claim)
    } yield userId

  /**
   * Extracts the user ID from an optional signed token.
   * Returns Emtpy if the token is not present.
   *
   * @param token The optional signed user token.
   * @return The optional user ID if the token is valid.
   * @throws Unauthorised if the token is invalid or expired.
   */
  def user(token: Maybe[User.SignedToken]): Maybe[User.Id] < (Sync & Abort[Unauthorised]) =
    token match {
      case Maybe.Present(token) => user(token).map(Maybe.Present(_))
      case Maybe.Absent         => Maybe.Absent
    }

  /**
   * Encodes a user ID into a signed JWT token with an expiration.
   *
   * @param userId The user ID to encode.
   * @return The signed user token.
   */
  def encodeToken(userId: User.Id): User.SignedToken < Sync =
    clock.now.map { now =>
      val expires = now.toJava.plusSeconds(config.tokenTtl.toSeconds).getEpochSecond
      val claim   = JwtClaim(subject = Some(userId.toString), expiration = Some(expires))
      User.SignedToken(JwtCirce.encode(claim, config.tokenSalt, JwtAlgorithm.HS256))
    }

  /**
   * Decodes and validates a signed JWT token.
   *
   * @param token The signed user token.
   * @return The decoded JWT claim.
   * @throws Unauthorised if the token is invalid.
   */
  def decodeToken(token: User.SignedToken): JwtClaim < Abort[Unauthorised] =
    Abort.get {
      JwtCirce
        .decode(token.value, config.tokenSalt, Seq(JwtAlgorithm.HS256))
        .toOption
        .toRight(Unauthorised.InvalidToken)
    }

  /**
   * Verifies that a JWT claim has not expired.
   *
   * @param claim The JWT claim to check.
   * @throws Unauthorised if the claim is expired.
   */
  def verifyExpiration(claim: JwtClaim): Unit < (Sync & Abort[Unauthorised]) =
    clock.now.map { now =>
      claim.expiration.exists(_ > now.toJava.getEpochSecond) match
        case true  => ()
        case false => Abort.fail(Unauthorised.TokenExpired)
    }

  /**
   * Parses the user ID from a JWT claim's subject field.
   *
   * @param claim The JWT claim.
   * @return The user ID if parsing is successful.
   * @throws Unauthorised if the subject is missing or invalid.
   */
  private def parseUserId(claim: JwtClaim): User.Id < Abort[Unauthorised] =
    claim.subject.flatMap(userId => Try(UUID.fromString(userId)).toOption) match {
      case Some(uuid) => uuid
      case None       => Abort.fail(Unauthorised.InvalidTokenSubject)
    }
}

object AuthenticationService:

  /**
   * Configuration for the AuthenticationService.
   *
   * @param passwordSalt Salt used for password hashing.
   * @param tokenSalt    Secret used for signing JWT tokens.
   * @param tokenTtl     Time-to-live for JWT tokens.
   */
  case class Config(passwordSalt: String, tokenSalt: String, tokenTtl: Duration)
