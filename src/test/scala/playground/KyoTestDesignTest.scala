package playground

import kyo.{ Result, * }
import kyo.Ansi.*

import scala.util.control.NoStackTrace

object KyoTestDesignTest extends KyoApp {
  type TestName          = String
  type SuiteName         = String
  type SuiteLogic        = TestScenarios ?=> Unit < (Async & Scope)
  type TestLogic         = Any < (Abort[Any] & Check & Async & Scope)
  type TestResultEffect  = Result[CheckFailed, Unit] < (Async & Scope)
  type SuiteResultEffect = List[TestResult] < (Async & Scope)

  run {
    spec2
      .map { result =>
        result.collect {
          case TestResult(suite, test, Result.Failure(_)) => s"$suite should $test"
        }
      }
      .map { failedTests =>
        if failedTests.isEmpty
        then Console.printLine(Ansi.green("All tests passed!")) *> failedTests
        else
          Console.printLine(Ansi.red(s"")) *>
            Console.printLine(Ansi.red(s"Failed tests:\n ✗ ${failedTests.mkString(",\n✗ ")}")) *> failedTests
      }
      .map { failedTests =>
        if failedTests.nonEmpty
        then Abort.fail(TestFailed)
        else Kyo.unit
      }
  }

  override def onResult[E, A](result: Result[E, A])(using Render[Result[E, A]], AllowUnsafe): Unit =
    result match
      case Result.Success(_)                 => ()
      case Result.Error(ex: TestFailed.type) => exit(1)
      case Result.Error(ex: Throwable)       => throw ex

  def spec2: SuiteResultEffect =
    "this is a suite description" should {

      "this is a test description" in {
        for result <- Abort.run(42)
        yield Check.require(result == Result.succeed(42), s"Expected 42 but got $result")
      }

      "this is a failing test description" in {
        for result <- Abort.run(42)
        yield Check.require(result == Result.succeed(43), s"Expected 43 but got $result")
      }
    }

  def withDb[A](fn: String => A): A =
    val db = "db connection"
    fn(db)

  def spec: SuiteResultEffect = suite("test suite") {
    for
      i <- 42: Int < Sync
      s <- "hello": String < Sync
    yield {

      // A simple test that runs a Kyo effect and asserts the result
      test("simple test") {
        for result <- Abort.run(42)
        yield Check.require(result == Result.succeed(42), s"Expected 42 but got $result")
      }

      // A failing test that runs a Kyo effect and asserts the result
      test("failing test") {
        for result <- Abort.run(42)
        yield Check.require(result == Result.succeed(43), s"Expected 43 but got $result")
      }
    }
  }

  class TestScenarios(var tests: List[(TestName, TestResultEffect)]):
    def add(name: TestName)(effect: TestResultEffect): Unit =
      tests = (name -> effect) :: tests

  object TestScenarios:
    def get(using builder: TestScenarios): TestScenarios = builder

  def suite(suiteName: SuiteName)(tests: TestScenarios ?=> Unit < (Async & Scope)): SuiteResultEffect = {
    given builder: TestScenarios = TestScenarios(Nil)
    val _                        = tests

    Loop(List.empty[TestResult], TestScenarios.get.tests.reverse) {
      case (acc, Nil)                  => Loop.done(acc)
      case (acc, (name, test) :: rest) =>
        for result <- test.tap(printTestResult(name, _))
        yield Loop.continue(TestResult(suiteName, name, result) :: acc, rest)
    }
  }

  def test(name: TestName)(testLogic: TestLogic): TestScenarios ?=> Unit =
    TestScenarios.get.add(name) {
      Abort.run:
        Check.runAbort:
          Abort
            .run(testLogic.unit)
            .map(result => Check.require(result.isSuccess, s"error $result"))
    }

  private def printTestResult(testName: TestName, result: Result[CheckFailed, Unit]): Unit < Sync =
    result match
      case Result.Success(_)     =>
        Console.printLine(Ansi.green(s"✓ $testName"))
      case Result.Failure(check) =>
        Console.printLine(s"✗ $testName".red) *>
          Console.printLine("  - ".red + check.message.red) *>
          Console.printLine(check.frame.render)

  extension (description: String) {
    infix def in(effect: TestLogic): TestScenarios ?=> Unit = test(description)(effect)
    infix def should(tests: SuiteLogic): SuiteResultEffect  = suite(description)(tests)
  }

  case class TestResult(suiteName: SuiteName, testName: TestName, result: Result[CheckFailed, Unit])

  object TestFailed extends NoStackTrace:
    override def getMessage: String = "Test failed"
}
