package conduit.infrastructure.codecs.database

import com.augustnagro.magnum.DbCodec
import kyo.Maybe

import java.net.URI
import java.sql.{ PreparedStatement, ResultSet, Timestamp, Types }
import java.time.Instant
import java.util.UUID
import scala.reflect.ClassTag

object DatabaseCodecs:
  /** A custom exception type for errors that occur during decoding of database values.
    * This exception is thrown when a value read from the database cannot be properly decoded
    * into the expected Scala type. It includes a message that provides details about the decoding error.
    * @param message a descriptive message about the decoding error
    */
  case class DecodeError(message: String) extends RuntimeException(message)

  /** A helper object for raising decoding errors. This provides a convenient method to throw a DecodeError with a specific message.
    * The raise method takes a message string and throws a DecodeError with that message, allowing for consistent error handling when decoding fails.
    */
  object DecodeError:
    /** Raises a DecodeError with the provided message. This method is used to signal that a decoding operation has failed due to an unexpected value or type.
      * @param message a descriptive message about the decoding error
      * @throws DecodeError always throws a DecodeError with the provided message
      */
    def raise(message: String): Nothing = throw DecodeError(message)

  /** A codec for converting between SQL array types and Scala List[String]. 
   * This codec allows you to read and write lists of strings
   */
  given DbCodec[List[String]] with {

    /* The SQL type for this codec is an array, which corresponds to the Java SQL type for arrays.
     * This indicates that when writing to the database, the codec will handle values as SQL arrays,
     * and when reading from the database, it will expect to read SQL arrays
     * that can be converted to Scala lists of strings. */
    val cols: IArray[Int] = IArray(Types.ARRAY)

    /* The query representation for this codec. */
    def queryRepr: String = "?"

    /**
     * Reads a List[String] from the ResultSet at the specified position. 
     * It handles SQL array types and converts them to Scala lists.
     */
    def readSingle(rs: ResultSet, pos: Int): List[String] =
      Option(rs.getArray(pos)) match
        case None        => Nil
        case Some(array) =>
          array.getArray match
            case array: Array[Any] =>
              array.toList.map {
                case str: String => str
                case other       => DecodeError.raise(s"Expected String but got ${other.getClass.getName}")
              }
            case other             => DecodeError.raise(s"Expected Array but got ${other}")

    /* Writes a List[String] to the PreparedStatement at the specified position.
     * It converts the Scala list to a SQL array that can be stored in the database. */
    def writeSingle(entity: List[String], ps: PreparedStatement, pos: Int): Unit =
      ps.setArray(pos, ps.getConnection.createArrayOf("text", entity.toArray))
  }

  /* A codec for converting between java.sql.Timestamp and java.time.Instant. */
  given DbCodec[Instant] = DbCodec[Timestamp].biMap(
    timestamp => timestamp.toInstant,
    instant => Timestamp.from(instant),
  )

  /**
   * A codec for TEXT columns mapped to java.net.URI, storing URIs as strings.
   *
   * The reader is null-tolerant because Magnum's `OptionCodec` calls `readSingle`
   * unconditionally and only checks `rs.wasNull()` afterward, so for a NULL
   * column the underlying `DbCodec[String]` returns `null` which must not blow
   * up the biMap.
   */
  given DbCodec[URI] =
    DbCodec[String].biMap(
      s => if s == null then null else URI.create(s),
      _.toString,
    )

  /**
   * Generic codec for kyo's `Maybe[A]`, derived from `DbCodec[Option[A]]` — which
   * Magnum provides for free for any `A` that has a `DbCodec`. This single derivation
   * covers `Maybe[String]`, `Maybe[URI]`, and any other nullable column type.
   */
  given [A](using DbCodec[Option[A]]): DbCodec[Maybe[A]] =
    DbCodec[Option[A]].biMap(Maybe.fromOption, _.toOption)

  /** A codec for PostgreSQL UUID array columns, used with = ANY(?) queries. */
  given listUUID: DbCodec[List[UUID]] with {
    val cols: IArray[Int]                                                      = IArray(Types.ARRAY)
    def queryRepr: String                                                      = "?"
    def readSingle(rs: ResultSet, pos: Int): List[UUID]                        =
      Option(rs.getArray(pos)).fold(Nil) { arr =>
        arr
          .getArray
          .asInstanceOf[Array[Object]]
          .map {
            case u: UUID => u
            case other   => DecodeError.raise(s"Expected UUID but got ${other.getClass.getName}")
          }
          .toList
      }
    def writeSingle(entity: List[UUID], ps: PreparedStatement, pos: Int): Unit =
      ps.setArray(pos, ps.getConnection.createArrayOf("uuid", entity.toArray))
  }
