package com.andremeira.test

import kyo.*
import sbt.testing.*

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

  override def name(): String = "kyo-test"

  override def fingerprints(): Array[Fingerprint] = Array(
    new SubclassFingerprint {
      override def isModule: Boolean                  = true
      override def superclassName(): String           = "com.andremeira.test.KyoTestSuite"
      override def requireNoArgConstructor(): Boolean = false
    }
  )

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

  override def tasks(taskDefs: Array[TaskDef]): Array[Task] =
    taskDefs.map(td => new KyoTask(td, classLoader))

  override def done(): String = ""
}

/**
 * sbt test task that executes a single [[KyoTestSuite]] and reports results
 * back through sbt's [[EventHandler]].
 */
class KyoTask(val taskDef: TaskDef, classLoader: ClassLoader) extends Task {

  override def tags(): Array[String] = Array.empty

  override def execute(handler: EventHandler, loggers: Array[Logger]): Array[Task] = {
    import AllowUnsafe.embrace.danger
    given Frame = Frame.derive

    val fqn       = taskDef.fullyQualifiedName()
    val startTime = java.lang.System.currentTimeMillis()

    try {
      // Load the Scala object (MODULE$ singleton)
      val moduleClass = classLoader.loadClass(fqn + "$")
      val raw         = moduleClass.getField("MODULE$").get(null)

      // Skip the KyoTestSuite companion object and any non-KyoTestSuite objects
      if !raw.isInstanceOf[KyoTestSuite] then return Array.empty

      val instance = raw.asInstanceOf[KyoTestSuite]

      // Run the Kyo specSuite effect synchronously
      val result: Result[Throwable, KyoTestSuite.SuiteResult] =
        KyoApp.Unsafe.runAndBlock(Duration.Infinity)(instance.specSuite)

      result match {
        case Result.Success(testResults) =>
          testResults.foreach { tr =>
            val testName = s"${tr.suiteName} should ${tr.testName}"
            val status   = tr.result match {
              case Result.Success(_) => Status.Success
              case _                 => Status.Failure
            }
            val duration = java.lang.System.currentTimeMillis() - startTime

            // Log to console (same format as KyoTestSuite)
            tr.result match {
              case Result.Success(_)     =>
                loggers.foreach(_.info(Ansi.green(s"✓ $testName")))
              case Result.Failure(check) =>
                loggers.foreach { l =>
                  l.info(Ansi.red(s"✗ $testName"))
                  l.info("  - " + Ansi.red(check.message))
                  l.info(check.frame.render.replace("\u001b[32m", "\u001b[33m"))
                }
            }

            handler.handle(KyoEvent(taskDef, testName, status, duration))
          }

        case Result.Error(ex: Throwable) =>
          val duration = java.lang.System.currentTimeMillis() - startTime
          loggers.foreach(_.error(s"Suite $fqn failed with exception: ${ex.getMessage}"))
          handler.handle(KyoEvent(taskDef, fqn, Status.Error, duration, Some(ex)))

        case Result.Panic(ex) =>
          val duration = java.lang.System.currentTimeMillis() - startTime
          loggers.foreach(_.error(s"Suite $fqn panicked: ${ex.getMessage}"))
          handler.handle(KyoEvent(taskDef, fqn, Status.Error, duration, Some(ex)))

        case other =>
          val duration = java.lang.System.currentTimeMillis() - startTime
          loggers.foreach(_.error(s"Suite $fqn returned unexpected result: $other"))
          handler.handle(KyoEvent(taskDef, fqn, Status.Error, duration))
      }

    }
    catch {
      case ex: Throwable =>
        val duration = java.lang.System.currentTimeMillis() - startTime
        loggers.foreach(_.error(s"Suite $fqn threw: ${ex.getMessage}"))
        handler.handle(KyoEvent(taskDef, fqn, Status.Error, duration, Some(ex)))
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
