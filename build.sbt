val scala3Version = "3.8.2"

lazy val root = project
  .in(file("."))
  .settings(
    TestTask.settings,
    name         := "conduit-kyo-postgres",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      // core
      "dev.zio"              %% "zio-prelude"     % "1.0.0-RC46",
      "io.getkyo"            %% "kyo-prelude"     % "1.0-RC1",
      "io.getkyo"            %% "kyo-core"        % "1.0-RC1",
      "io.getkyo"            %% "kyo-combinators" % "1.0-RC1",
      "ch.qos.logback"        % "logback-classic" % "1.5.32",
      "commons-codec"         % "commons-codec"   % "1.21.0",
      "com.github.jwt-scala" %% "jwt-circe"       % "11.0.3",

      // database
      "io.getquill"     %% "quill-jdbc" % "4.8.6",
      "org.postgresql"   % "postgresql" % "42.7.10",
      "com.augustnagro" %% "magnum"     % "1.3.1",

      // test
      "org.scalameta" %% "munit" % "1.2.1" % Test,
      // We need this to run the kyo tests with sbt
      // "dev.zio"       %% "zio-test"     % "2.1.24"  % Test,
      // "dev.zio"       %% "zio-test-sbt" % "2.1.24"  % Test,
      // "io.getkyo"     %% "kyo-zio-test" % "1.0-RC1" % Test,
    ),
    scalacOptions ++= Seq(
      "-Wvalue-discard",
      "-Wnonunit-statement",
      "-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error",
    ),
  )
