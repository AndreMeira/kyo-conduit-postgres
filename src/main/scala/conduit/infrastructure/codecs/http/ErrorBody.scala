package conduit.infrastructure.codecs.http

import sttp.model.StatusCode

/** HTTP error response — a concrete (non-generic) type so Kyo can derive a ConcreteTag.
  *
  * Matches the Conduit spec format:
  * {{{
  *   { "errors": { "body": ["can't be empty", "is too short"] } }
  * }}}
  *
  * @param status  the HTTP status code to return
  * @param errors  a map of field names to lists of error messages
  */
case class ErrorBody(
  status: StatusCode,
  errors: Map[String, List[String]],
)
