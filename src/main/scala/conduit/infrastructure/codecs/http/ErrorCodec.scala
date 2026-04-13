package conduit.infrastructure.codecs.http

import conduit.domain.error.{ ApplicationError, ValidationError }
import sttp.model.StatusCodes

object ErrorCodec extends StatusCodes {
  def encode(error: ApplicationError): ErrorBody = error match
    case e: ApplicationError.NotFoundError     => ErrorBody(NotFound, Map(e.kind -> List(e.message)))
    case e: ApplicationError.UnauthorisedError => ErrorBody(Forbidden, Map(e.kind -> List(e.message)))
    case e: ValidationError                    => ErrorBody(BadRequest, Map(e.kind -> List(e.message)))
    case e                                     => ErrorBody(InternalServerError, Map(e.kind -> List(e.message)))
}
