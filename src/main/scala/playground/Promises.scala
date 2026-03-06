package playground

import kyo.*

import scala.concurrent.duration.given

object Promises extends KyoApp {

  def resolve(promise: Promise[Int, Any]): Unit < Async =
    Kyo.sleep(2.seconds) *> promise.complete(Result.succeed(42)).map(_ => ())

  run {
    for {
      promise <- Promise.init[Int, Any]
      _       <- Console.printLine("Resolving promise in 2 seconds...")
      c       <- Scope.acquireRelease(resolve(promise).fork)(_.get)
      r       <- promise.get
      _       <- Console.printLine(s">>>>>>>>>>>>>>>>>> $r")
    } yield ()
  }
}
