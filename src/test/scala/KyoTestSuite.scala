package com.andremeira.test

import kyo.*
import kyo.Ansi.*
import KyoTestSuite.*

import scala.util.control.NoStackTrace

/**
 * Abstract base class for defining test suites using the Kyo framework.
 * Provides utilities for defining and running test suites, individual tests,
 * and assertions with structured reporting of results.
 */
abstract class KyoTestSuite extends KyoApp {

  /**
   * Defines the test suite to be executed.
   * @return A suite result containing the results of all tests in the suite.
   */
  def specSuite: SuiteResult < (Async & Scope)

  /**
   * Runs the test suite and prints the results to the console.
   */
  run {
    Clock.now.map { instant =>
      specSuite
        .map: result =>
          result.collect:
            case TestResult(suite, test, Result.Failure(_)) => s"$suite should $test"
        .map: failedTests =>
          Clock.now.map { end =>
            val duration = end - instant
            Console.printLine(s"\nTest suite completed in ${duration.toMillis}ms".bold) *> failedTests
          }
        .map:
          case Nil         => printSuiteResult(Nil) *> Kyo.unit
          case failedTests => printSuiteResult(failedTests) *> Abort.fail(TestFailed)
    }
  }

  /**
   * Defines a suite of tests.
   * @param suiteName The name of the suite.
   * @param tests The logic for the suite, which includes the individual tests.
   * @return A suite result containing the results of all tests in the suite.
   */
  def suite(suiteName: SuiteName)(tests: SuiteLogic): SuiteResult < (Async & Scope) = {
    given builder: TestScenarios = TestScenarios(Nil)

    tests *> Loop(List.empty[TestResult], TestScenarios.get.tests.reverse) {
      case (acc, Nil)                  => Loop.done(acc)
      case (acc, (name, test) :: rest) =>
        for result <- test.tap(printTestResult(s"$suiteName should $name", _))
        yield Loop.continue(TestResult(suiteName, name, result) :: acc, rest)
    }
  }

  /**
   * Defines an individual test within a suite.
   * @param name The name of the test.
   * @param testLogic The logic for the test.
   */
  def test(name: TestName)(testLogic: TestLogic): TestScenarios ?=> Unit =
    TestScenarios.get.add(name) {
      Abort.run:
        Check.runAbort:
          Abort
            .run(testLogic.unit)
            .map(result => Check.require(result.isSuccess, s"error $result"))
    }

  /**
   * Represents a successful test result.
   * @return A unit effect within the Check context.
   */
  def ok: Unit < Check = Kyo.unit

  /**
   * Asserts a condition within a test.
   * @param condition The condition to assert.
   * @param message The message to display if the assertion fails.
   * @param frame The frame for rendering the assertion context.
   */
  inline def assert(
    inline condition: Boolean,
    inline message: => String,
  )(using inline frame: Frame
  ): Unit < Check =
    Check.require(condition, message)

  /**
   * Prints the result of an individual test.
   * @param testName The name of the test.
   * @param result The result of the test.
   */
  private def printTestResult(testName: TestName, result: Result[CheckFailed, Unit]): Unit < Sync =
    result match
      case Result.Success(_)     =>
        Kyo.unit
          *> Console.printLine(Ansi.green(s"✓ $testName"))
      case Result.Failure(check) =>
        Kyo.unit
          *> Console.printLine(s"✗ $testName".red)
          *> Console.printLine("  - ".red + check.message.red)
          *> Console.printLine(check.frame.render.replace("\u001b[32m", "\u001b[33m"))

  /**
   * Prints the results of the entire test suite.
   * @param failedTests A list of failed test names.
   */
  private def printSuiteResult(failedTests: List[String]): Unit < Sync =
    if failedTests.isEmpty
    then Console.printLine(Ansi.green("All tests passed!"))
    else Console.printLine(Ansi.red(s"\nFailed tests:\n ✗ ${failedTests.mkString(",\n ✗ ")}"))

  /**
   * Extension methods for defining tests and suites.
   */
  extension (description: String) {

    /**
     * Defines a test with a description.
     * @param effect The logic for the test.
     */
    infix def in(effect: TestLogic): TestScenarios ?=> Unit = test(description)(effect)

    /**
     * Defines a suite with a description.
     * @param tests The logic for the suite.
     */
    infix def should(tests: SuiteLogic): SuiteResultEffect = suite(description)(tests)
  }

  /**
   * Extension methods for combining assertions.
   */
  extension (assertion: Unit < Check) {

    /**
     * Combines two assertions.
     * @param other The other assertion to combine.
     * @return A combined assertion.
     */
    def &(other: Unit < Check): Unit < Check = assertion *> other
  }
}

object KyoTestSuite {
  type TestName          = String
  type SuiteName         = String
  type SuiteLogic        = TestScenarios ?=> Unit < (Async & Scope)
  type FixtureLogic[A]   = TestScenarios ?=> A => Unit < (Async & Scope)
  type TestLogic         = Any < (Abort[Any] & Check & Async & Scope)
  type SuiteResult       = List[TestResult]
  type TestResultEffect  = Result[CheckFailed, Unit] < (Async & Scope)
  type SuiteResultEffect = SuiteResult < (Async & Scope)

  /**
   * Represents the result of an individual test.
   * @param suiteName The name of the suite the test belongs to.
   * @param testName The name of the test.
   * @param result The result of the test.
   */
  case class TestResult(
    suiteName: SuiteName,
    testName: TestName,
    result: Result[CheckFailed, Unit],
  )

  /**
   * Manages the collection of test scenarios within a suite.
   * @param tests The list of test scenarios.
   */
  class TestScenarios(var tests: List[(TestName, TestResultEffect)]):
    /**
     * Adds a test scenario to the collection.
     * @param name The name of the test.
     * @param effect The logic for the test.
     */
    def add(name: TestName)(effect: TestResultEffect): Unit =
      tests = (name -> effect) :: tests

  object TestScenarios:
    /**
     * Retrieves the current TestScenarios instance.
     * @param builder The implicit TestScenarios instance.
     * @return The current TestScenarios instance.
     */
    def get(using builder: TestScenarios): TestScenarios = builder

  /**
   * Represents a failure in the test suite.
   */
  object TestFailed extends NoStackTrace:
    override def getMessage: String = "Test failed"
}
