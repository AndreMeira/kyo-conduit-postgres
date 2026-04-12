package com.andremeira.test

import kyo.*
import sbt.testing.*
import java.lang.System

/**
 * sbt test framework that discovers and runs [[KyoTestSuite]] objects.
 *
 * Register in build.sbt with:
 * {{{
 *   testFrameworks += new TestFramework("com.andremeira.test.KyoFramework")
 * }}}
 *
 * This lets you use `sbt test` and `sbt testOnly *FooSpec` instead of
 * `sbt "Test/runMain com.example.FooSpec"`.
 */
class KyoFramework extends Framework {

  /**
   * The name of the test framework, used by sbt to identify it in test options.
   *
   * @return a string identifier for this test framework, e.g. "kyo-test"
   */
  override def name(): String = "kyo-test"

  /**
   * Defines how sbt discovers test suites. This implementation looks for Scala
   * objects that extend [[KyoTestSuite]].
   *
   * @return an array of Fingerprints that sbt uses to identify test suites
   */
  override def fingerprints(): Array[Fingerprint] = Array(
    new SubclassFingerprint {
      override def isModule: Boolean                  = true
      override def superclassName(): String           = "com.andremeira.test.KyoTestSuite"
      override def requireNoArgConstructor(): Boolean = false
    }
  )

  /**
   * Creates a Runner that sbt uses to execute tests. The Runner will create
   * KyoTask instances for each discovered test suite.
   *
   * @param args command-line arguments passed to the test runner
   * @param remoteArgs arguments for remote execution (not used here)
   * @param testClassLoader the class loader to use for loading test classes
   * @return a Runner instance that sbt will use to run tests
   */
  override def runner(
    args: Array[String],
    remoteArgs: Array[String],
    testClassLoader: ClassLoader,
  ): Runner = new KyoRunner(args, remoteArgs, testClassLoader)
}

/**
 * sbt test runner that creates [[KyoTask]] instances for each discovered suite.
 */
class KyoRunner(
  val args: Array[String],
  val remoteArgs: Array[String],
  classLoader: ClassLoader,
) extends Runner {

  /**
   * Creates a KyoTask for each TaskDef that sbt has discovered. Each TaskDef
   * corresponds to a test suite that matches the Fingerprint defined in the
   * Framework.
   *
   * @param taskDefs an array of TaskDef objects representing discovered test suites
   * @return an array of Task instances that sbt will execute
   */
  override def tasks(taskDefs: Array[TaskDef]): Array[Task] =
    taskDefs.map(td => new KyoTask(td, classLoader))

  /**
   * Called by sbt when all tasks have completed. This implementation does nothing
   * and returns an empty string, but it could be used to perform any necessary
   * cleanup or final reporting after all tests have run.
   *
   * @return a string message to sbt (empty in this case)
   */
  override def done(): String = ""
}

/**
 * sbt test task that executes a single [[KyoTestSuite]] and reports results
 * back through sbt's [[EventHandler]].
 */
class KyoTask(val taskDef: TaskDef, classLoader: ClassLoader) extends Task {

  /**
   * Returns an array of tags for this task. This implementation returns an empty
   * array, but it could be extended to return tags based on the test suite or
   * other criteria.
   *
   * @return an array of strings representing tags for this task
   */
  override def tags(): Array[String] = Array.empty

  /**
   * Executes the test suite associated with this task. It loads the Scala object
   * for the test suite, runs its specSuite effect, and reports results back to
   * sbt through the EventHandler.
   *
   * @param handler the EventHandler to report test results to
   * @param loggers an array of Logger instances for logging output
   * @return an array of nested Task instances (empty in this case)
   */
  override def execute(handler: EventHandler, loggers: Array[Logger]): Array[Task] = {
    import AllowUnsafe.embrace.danger
    given Frame = Frame.derive

    val qualifiedName = taskDef.fullyQualifiedName()
    val startTime     = System.currentTimeMillis()

    try {
      // Load the Scala object (MODULE$ singleton)
      val moduleClass = classLoader.loadClass(qualifiedName + "$")
      val raw         = moduleClass.getField("MODULE$").get(null)

      // Skip the KyoTestSuite companion object and any non-KyoTestSuite objects
      if !raw.isInstanceOf[KyoTestSuite] then return Array.empty

      val instance = raw.asInstanceOf[KyoTestSuite]

      // Run the Kyo specSuite effect synchronously
      val result: Result[Throwable, KyoTestSuite.SuiteResult] =
        KyoApp.Unsafe.runAndBlock(Duration.Infinity)(instance.specSuite)

      result match {
        case Result.Success(testResults) =>
          testResults.foreach { testResult =>
            val testName = s"${testResult.suiteName} should ${testResult.testName}"

            // Log to console (same format as KyoTestSuite)
            testResult.result match
              case Result.Success(_)     =>
              // loggers.foreach(_.info(Ansi.green(s"✓ $testName")))
              case Result.Failure(check) =>
                loggers.foreach { l =>
                  l.info(Ansi.red(s"✗ $testName"))
                  l.info("  - " + Ansi.red(check.message))
                  l.info(check.frame.render.replace("\u001b[32m", "\u001b[33m"))
                }

            val status   = testResult.result match
              case Result.Success(_) => Status.Success
              case _                 => Status.Failure
            val duration = System.currentTimeMillis() - startTime
            handler.handle(KyoEvent(taskDef, testName, status, duration))
          }

        case Result.Error(ex: Throwable) =>
          val duration = System.currentTimeMillis() - startTime
          loggers.foreach(_.error(s"Suite $qualifiedName failed with exception: ${ex.getMessage}"))
          handler.handle(KyoEvent(taskDef, qualifiedName, Status.Error, duration, Some(ex)))

        case Result.Panic(ex) =>
          val duration = System.currentTimeMillis() - startTime
          loggers.foreach(_.error(s"Suite $qualifiedName panicked: ${ex.getMessage}"))
          handler.handle(KyoEvent(taskDef, qualifiedName, Status.Error, duration, Some(ex)))

        case other =>
          val duration = System.currentTimeMillis() - startTime
          loggers.foreach(_.error(s"Suite $qualifiedName returned unexpected result: $other"))
          handler.handle(KyoEvent(taskDef, qualifiedName, Status.Error, duration))
      }

    }
    catch {
      case ex: Throwable =>
        val duration = System.currentTimeMillis() - startTime
        loggers.foreach(_.error(s"Suite $qualifiedName threw: ${ex.getMessage}"))
        handler.handle(KyoEvent(taskDef, qualifiedName, Status.Error, duration, Some(ex)))
    }

    Array.empty // no nested tasks
  }
}

/**
 * Minimal sbt [[Event]] implementation for reporting individual test results.
 */
private class KyoEvent(
  td: TaskDef,
  testName: String,
  testStatus: Status,
  durationMs: Long,
  error: Option[Throwable] = None,
) extends Event {
  override def status(): Status               = testStatus
  override def duration(): Long               = durationMs
  override def fingerprint(): Fingerprint     = td.fingerprint()
  override def fullyQualifiedName(): String   = td.fullyQualifiedName()
  override def selector(): Selector           = new TestSelector(testName)
  override def throwable(): OptionalThrowable = error.fold(new OptionalThrowable())(e => new OptionalThrowable(e))
}
