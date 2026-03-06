package playground

import kyo.{ CheckFailed, * }
import Ansi.*

object SimpleKyoTestSketch extends KyoApp {
  run {
    for {
      r1 <- test("simple test"):
              Check.require(1 + 1 == 2, "Math is Ok")
      r2 <- test("failing test"):
              Check.require(1 + 1 == 3, "Math is still broken")
    } yield ()
  }

  def test[Effect](
    name: String
  )(
    effect: Unit < (Effect & Check)
  ): Result[CheckFailed, Unit] < (Effect & Sync) =
    Abort
      .run(runTest(effect))
      .tap:
        case result @ Result.Success(_)     =>
          Console.printLine(Ansi.green(s"✓ $name"))
        case result @ Result.Failure(check) =>
          Kyo.unit
            *> Console.printLine(s"✗ $name".red)
            *> Console.printLine("  - ".red + check.message.red)
            *> Console.printLine(check.frame.render)

  def runTest[Effect, A](
    effect: A < (Effect & Check)
  ): Unit < (Effect & Sync & Abort[CheckFailed]) = Check.runAbort:
    Abort.run(effect).map { result =>
      Check.require(result.isSuccess, s"error $result")
    }
}
