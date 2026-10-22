package conduit.infrastructure.postgres.magnum

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.error.ApplicationError
import conduit.infrastructure.postgres.PostgresTransaction.Transactional
import conduit.infrastructure.postgres.{ PostgresDatabase, PostgresTestSupport }
import kyo.*

object PostgresDatabaseSpec extends KyoTestSuite:

  /**
   * The failure a test aborts its transaction with.
   */
  case object Refused extends ApplicationError:
    override def message: String = "refused by the test"

  def specSuite: SuiteResult < (Async & Scope) =
    "PostgresDatabase" should withClients { (client, observer) =>
      val database = PostgresDatabase(client)

      "run separate statements of a transaction in the same database transaction" in
        database.transaction:
          for
            first  <- Transactional(currentTransactionId)
            second <- Transactional(currentTransactionId)
          yield assert(
            first.isDefined && first == second,
            s"Expected one transaction, got $first and $second",
          )

      "run separate transactions in separate database transactions" in {
        for
          first  <- database.transaction(Transactional(currentTransactionId))
          second <- database.transaction(Transactional(currentTransactionId))
        yield assert(
          first.isDefined && first != second,
          s"Expected two transactions, got $first and $second",
        )
      }

      "run statements of fibers forked in a transaction in that transaction" in
        database.transaction:
          for
            outer  <- Transactional(currentTransactionId)
            forked <- Async.zip(Transactional(currentTransactionId), Transactional(currentTransactionId))
          yield assert(
            outer.isDefined && forked == (outer, outer),
            s"Expected $outer in both fibers, got $forked",
          )

      "keep its writes invisible to other connections until it commits" in {
        for
          before <- database.transaction:
                      Transactional(insertProbe(1)) *> DB.run(observer)(probesWithId(1))
          after  <- DB.run(observer)(probesWithId(1))
        yield assert(
          before.contains(0L) && after.contains(1L),
          s"Expected 0 rows before commit and 1 after, got $before and $after",
        )
      }

      "roll back every statement when the transaction aborts" in {
        for
          result <- Abort.run[ApplicationError]:
                      database.transaction:
                        Kyo.unit
                          *> Transactional(insertProbe(2))
                          *> Transactional(insertProbe(2))
                          *> Abort.fail[ApplicationError](Refused)
          seen   <- DB.run(observer)(probesWithId(2))
        yield assert(
          result == Result.fail(Refused) && seen.contains(0L),
          s"Expected Refused and 0 rows, got $result and $seen",
        )
      }

      "abort with QueryError when a statement fails" in {
        for result <- Abort.run[ApplicationError] {
                        database.transaction(Transactional(sql"SELECT * FROM missing_table".execute))
                      }
        yield assert(result.failure.exists(isQueryError), s"Expected QueryError, got $result")
      }
    }

  /**
   * Provides two clients to the same live database, which holds an empty `probe (id INT)` table:
   * the one under test and an observer that never shares its transactions.
   *
   * @param test the suite body, given the client under test and the observer
   * @tparam A the result of the suite body
   * @return the result of the suite body
   */
  def withClients[A](test: (PostgresClient, PostgresClient) => A < (Async & Scope)): A < (Async & Scope) =
    PostgresTestSupport.withConnectionUrl { url =>
      for
        client   <- PostgresClient.init(url).orPanic
        observer <- PostgresClient.init(url).orPanic
        _        <- DB.run(client)(DB.executeRaw("CREATE TABLE probe (id INT NOT NULL)")).orPanic
        result   <- test(client, observer)
      yield result
    }

  /**
   * The id of the database transaction the statement runs in.
   *
   * @return the transaction id; aborts with `SqlException` when the statement fails
   */
  def currentTransactionId: Option[Long] < (DB & Abort[SqlException]) =
    sql"SELECT txid_current()".as[Long].run.map(_.headOption)

  /**
   * Inserts one probe row.
   *
   * @param id the id of the row
   * @return the number of inserted rows; aborts with `SqlException` when the statement fails
   */
  def insertProbe(id: Int): Long < (DB & Abort[SqlException]) =
    sql"INSERT INTO probe (id) VALUES ($id)".execute

  /**
   * Counts the probe rows with the given id visible to the client running the statement.
   *
   * @param id the id of the rows to count
   * @return the count; aborts with `SqlException` when the statement fails
   */
  def probesWithId(id: Int): Option[Long] < (DB & Abort[SqlException]) =
    sql"SELECT count(*) FROM probe WHERE id = $id".as[Long].run.map(_.headOption)

  /**
   * Whether the failure is a `PostgresDatabase.Error.QueryError`.
   *
   * @param error the failure
   * @return true for a `QueryError`
   */
  def isQueryError(error: ApplicationError): Boolean = error match
    case PostgresDatabase.Error.QueryError(_) => true
    case _                                    => false
