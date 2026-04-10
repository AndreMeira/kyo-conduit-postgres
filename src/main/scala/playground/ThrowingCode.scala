package playground

import kyo.*

object ThrowingCode extends KyoApp {
  def test: Int < (Sync & Abort[Exception]) =
    Sync.defer {
      for {
        i <- Random.nextInt
        _ <- Abort.catching(throw new RuntimeException("oops"))
      } yield i
    }

  run {
    test
  }
}
