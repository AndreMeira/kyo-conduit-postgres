val scala3Version = "3.7.4"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "conduit-kyo-postgres",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      // core
      "dev.zio"       %% "zio-prelude"     % "1.0.0-RC44",
      "io.getkyo"     %% "kyo-prelude"     % "1.0-RC1",
      "io.getkyo"     %% "kyo-core"        % "1.0-RC1",
      "io.getkyo"     %% "kyo-combinators" % "1.0-RC1",
      "ch.qos.logback" % "logback-classic" % "1.5.23",

      // database
      "io.getquill"     %% "quill-jdbc"  % "4.8.6",
      "org.postgresql"   % "postgresql"  % "42.7.8",
      "org.scalikejdbc" %% "scalikejdbc" % "4.3.5",

      // test
      "org.scalameta" %% "munit" % "1.2.1" % Test,
    ),
    scalacOptions ++= Seq(
      "-Wvalue-discard",
      "-Wnonunit-statement",
      "-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error",
    ),
  )
