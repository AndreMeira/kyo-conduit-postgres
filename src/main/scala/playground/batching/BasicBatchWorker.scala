package playground.batching

import conduit.domain.error.ApplicationError
import BasicBatchWorker.{ BatchProcessor, Error }
import kyo.*

class BasicBatchWorker[A, B](
  batchSize: Int,
  processor: BatchProcessor[A, B],
  channel: Channel[(A, Promise[B, Abort[ApplicationError]])],
) {
  def apply(value: A): B < (Abort[ApplicationError] & Async) =
    for {
      promise <- Promise.init[B, Abort[ApplicationError]]
      _       <- channel.put(value -> promise).mapAbort(_ => Error.WorkerChannelClosed)
      result  <- promise.get
    } yield result

  def start: Unit < (Async & Abort[ApplicationError]) =
    for {
      elems      <- channel.drainUpTo(batchSize).mapAbort(_ => Error.WorkerChannelClosed)
      batch       = Batched.fromSeq(elems.toList)
      data        = batch.map((data, _) => data)
      promises    = batch.map((_, promise) => promise)
      result     <- processor(data)
      processed   = result.zip(promises)
      unprocessed = promises -- result
      _          <- Kyo.foreach(unprocessed.values)(_.complete(Result.fail(Error.Unprocessed)))
      _          <- Kyo.foreach(processed.values):
                      case Right(value) -> promise => promise.complete(Result.succeed(value))
                      case Left(error) -> promise  => promise.complete(Result.fail(error))
    } yield ()
}

object BasicBatchWorker:
  type BatchProcessor[A, B] = Batched[A] => Batched[Either[ApplicationError, B]] < (Async & Abort[ApplicationError])

  enum Error extends ApplicationError:
    case Unprocessed
    case WorkerChannelClosed

    override def message: String = this match
      case Unprocessed         => "Unprocessed batch element"
      case WorkerChannelClosed => "Worker channel is closed"
