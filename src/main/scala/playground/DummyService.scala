package playground

import kyo.*
trait DummyService {
  type Req
  def test: Int < Req
}

object DummyService:
  class Impl extends DummyService {
    type Req = Sync & Abort[Throwable]

    def test: Int < Req = for {
      i <- Random.nextInt
      j <- Kyo.lift(3)
      _ <- if i < 10 then Kyo.unit
           else Abort.fail(RuntimeException("too big"))
    } yield i + j

    def ever: Nothing < (Async & Abort[Throwable]) = test.forever
  }
