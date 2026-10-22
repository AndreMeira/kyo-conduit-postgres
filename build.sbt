val scala3Version = "3.9.0"

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    TestTask.settings,
    name                := "conduit-kyo-postgres",
    version             := "0.1.0-SNAPSHOT",
    scalaVersion        := scala3Version,
    libraryDependencies ++= Seq(
      // core
      "dev.zio"              %% "zio-prelude"     % "1.0.0-RC48",
      "io.getkyo"            %% "kyo-prelude"     % "1.0.0-RC7",
      "io.getkyo"            %% "kyo-core"        % "1.0.0-RC7",
      "io.getkyo"            %% "kyo-http"        % "1.0.0-RC7",
      "io.getkyo"            %% "kyo-actor"       % "1.0.0-RC7",
      "io.getkyo"            %% "kyo-combinators" % "1.0.0-RC7",
      "ch.qos.logback"        % "logback-classic" % "1.6.5",
      "commons-codec"         % "commons-codec"   % "1.23.0",
      "com.github.jwt-scala" %% "jwt-circe"       % "12.0.0",
      "com.typesafe"          % "config"          % "1.4.9",

      // database
      "org.postgresql"   % "postgresql"                 % "42.7.14",
      "com.zaxxer"       % "HikariCP"                   % "7.1.0",
      "com.augustnagro" %% "magnum"                     % "1.3.1",
      "com.augustnagro" %% "magnumpg"                   % "1.3.1",
      "org.flywaydb"     % "flyway-core"                % "13.11.1",
      "org.flywaydb"     % "flyway-database-postgresql" % "13.11.1",
      "io.getkyo"       %% "kyo-sql"                    % "1.0.0-RC7",
      "io.getkyo"       %% "kyo-sql-postgres"           % "1.0.0-RC7",

      // config
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.11",

      // json
      "io.circe" %% "circe-generic" % "0.14.17",
      "io.circe" %% "circe-core"    % "0.14.17",
      "io.circe" %% "circe-parser"  % "0.14.17",

      // test
      "org.scalameta"     %% "munit"      % "1.3.6"  % Test,
      "org.testcontainers" % "postgresql" % "1.21.4" % Test,
    ),
    Compile / mainClass := Some("conduit.application.http.HttpApplication"),
    // sbt 2 nests target per platform and Scala version; the Dockerfile copies the stage from here
    Universal / sbt.Keys.stagingDirectory :=
      baseDirectory.value / "target" / "universal" / "stage",
    testFrameworks      := Seq(new TestFramework("com.andremeira.test.KyoFramework")),
    scalacOptions ++= Seq(
      "-Wvalue-discard",
      "-Wnonunit-statement",
      "-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error",
    ),
  )
