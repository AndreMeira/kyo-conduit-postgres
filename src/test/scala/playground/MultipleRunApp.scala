package playground

import kyo.*

object MultipleRunApp extends KyoApp {

  run {
    for {
      _ <- Console.printLine("First run")
      _ <- Console.printLine("Second run")
      _ <- Abort.fail(new RuntimeException("Abort in first run"))
    } yield ()
  }

  run {
    for {
      _ <- Console.printLine("Third run")
      _ <- Console.printLine("Fourth run")
    } yield ()
  }
}
