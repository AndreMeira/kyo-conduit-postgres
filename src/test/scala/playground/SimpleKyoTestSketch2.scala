package playground

import kyo.Ansi.*
import kyo.{ CheckFailed, * }

object SimpleKyoTestSketch2 extends KyoApp {
  opaque type TestName  = String
  opaque type SuiteName = String


  def specSuite = suite("test suite")
    *> test("simple test") {
      for {
        _ <- Console.printLine("Running toto")
        _ <- Kyo.sleep(1.second)
      } yield ok
        & assert(true, "toto should pass")
        & assert(false, "toto should fail")
    }
    *> test("failing test") {
      ok
        & assert(1 + 1 == 2, "Math is Ok")
        & assert(1 + 1 == 3, "Math is still broken")
    }

  def suite(str: String): Unit < Any = ???

  def resource(a: Any): Nothing = ???

  run {
    for {
      toto <- toto
      r1   <- test("simple test"):
                assert(1 + 1 == 2, "Math is Ok")
      r2   <- test("failing test"):
                ok
                  & assert(1 + 1 == 2, "Math is Ok")
                  & assert(1 + 1 == 3, "Math is still broken")
    } yield ()
  }

  def toto: Result[CheckFailed, Unit] < (Async & Sync) = test("toto"):
    for {
      _ <- Console.printLine("Running toto")
      _ <- Kyo.sleep(1.second)
    } yield ok
      & assert(true, "toto should pass")
      & assert(false, "toto should fail")

  def test[Effect](
    name: String
  )(
    effect: Int => Unit < (Effect & Check)
  ): Result[CheckFailed, Unit] < (Effect & Async) = test(name)(effect(43))

  def test[Effect](
    name: String
  )(
    effect: Unit < (Effect & Check)
  ): Result[CheckFailed, Unit] < (Effect & Sync) =
    Abort
      .run(Check.runAbort {
        Abort.run(effect).map { result =>
          Check.require(result.isSuccess, s"error $result")
        }
      })
      .tap:
        case result @ Result.Success(_)     =>
          Console.printLine(Ansi.green(s"✓ $name"))
        case result @ Result.Failure(check) =>
          Kyo.unit
            *> Console.printLine(s"✗ $name".red)
            *> Console.printLine("  - ".red + check.message.red)
            *> Console.printLine(check.frame.render)

  def ok: Unit < Check = Kyo.unit

  inline def assert(
    inline condition: Boolean,
    inline message: => String,
  )(using inline frame: Frame
  ): Unit < Check =
    Check.require(condition, message)

  extension (assertion: Unit < Check) def &(other: Unit < Check): Unit < Check = assertion *> other

  sealed trait SpecResult:
    def combine(other: SpecResult): SpecResult =
      (this, other) match {
        case (SpecResult.Suite(name1, results1), SpecResult.Suite(name2, results2)) =>
          SpecResult.Suite(s"$name1 + $name2", results1 ++ results2)
        case (SpecResult.Suite(name, results), single: SpecResult.Test)             =>
          SpecResult.Suite(name, results :+ single)
        case (single: SpecResult.Test, SpecResult.Suite(name, results))             =>
          SpecResult.Suite(name, single +: results)
        case (single1: SpecResult.Test, single2: SpecResult.Test)                   =>
          SpecResult.Suite(s"${single1.name} + ${single2.name}", List(single1, single2))
      }

    def toList: List[Result[CheckFailed, Unit]] =
      this match
        case SpecResult.Suite(_, results) => results.flatMap(_.toList)
        case SpecResult.Test(_, result)   => List(result)

  object SpecResult:
    case class Suite(name: SuiteName, results: List[SpecResult.Test])  extends SpecResult
    case class Test(name: TestName, result: Result[CheckFailed, Unit]) extends SpecResult
}
