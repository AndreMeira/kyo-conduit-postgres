// project/MyTasks.scala
import sbt.*
import sbt.Keys.*
import complete.DefaultParsers.*

object TestTask {
  lazy val runTest = inputKey[Unit]("Run a main class from test sources")

  lazy val settings: Seq[Setting[?]] = Seq(
    runTest / fork          := true,
    runTest / fullClasspath := (Test / fullClasspath).value,
    runTest                 := {
      val mainClass = spaceDelimited("<main class>").parsed.head
      val cp        = (runTest / fullClasspath).value
      val r         = (runTest / runner).value
      val log       = streams.value.log
      r.run(mainClass, cp.files, Seq.empty, log).get
    },
  )
}
