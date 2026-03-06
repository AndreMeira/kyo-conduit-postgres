package playground

import kyo.*
import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.{ FixtureLogic, SuiteLogic, SuiteResult, TestLogic, TestScenarios }

object SimpleTestSuite extends KyoTestSuite {

  override def specSuite: SuiteResult < (Async & Scope) =
    "String" should withClient { client =>

      "have the right length" in {
        for {
          hello   <- Kyo.lift("Hello")
          goodbye <- Kyo.lift("Goodbye")
        } yield ok
          & assert(hello.length == 5, s"Expected length 5 but got ${hello.length}")
          & assert(goodbye.length == 7, s"Expected length 7 but got ${goodbye.length}")
      }

      "contain characters" in {
        for {
          str <- Kyo.lift("Hello")
        } yield assert(str.contains("H"), s"Expected string to contain 'H'")
      }

      "have the right client count" in {
        for {
          count <- client.get
        } yield assert(count == 1, s"Expected client count to be 1 but got $count")
      }

      "have a clean state between tests" in {
        for {
          count <- client.get
        } yield assert(count == 1, s"Expected client count to be 1 but got $count")
      }
    }

  def withClient[A, S](fn: Client => SuiteLogic): SuiteLogic =
    for client <- client yield fn(client)

  def client: Client < (Sync & Scope) =
    for
      _      <- Console.printLine("Setting up client fixture")
      ref    <- AtomicRef.init(0)
      client <- Scope.acquireRelease(new Client(ref).tap(_.increment))(_.decrement)
    yield client

  class Client(ref: AtomicRef[Int]) {
    def increment: Unit < Sync = ref.updateAndGet(_ + 1).unit
    def decrement: Unit < Sync = ref.updateAndGet(_ - 1).unit
    def get: Int < Sync        = ref.get
  }
}
