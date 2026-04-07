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
      val classpath = (runTest / fullClasspath).value
      val run       = (runTest / runner).value
      val log       = streams.value.log
      run.run(mainClass, classpath.files, Seq.empty, log).get
    },
  )
}
