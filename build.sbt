name := "JobForce"

version := "1.0"

scalaVersion := "2.12.2"

val catsVersion = "0.9.0"
val scalacheckVersion = "1.13.5"
val scalacheckCatsVersion = "0.3.3"
val specs2Version = "3.9.2"
val fs2Version = "0.9.6"
val fs2CatsVersion = "0.3.0"
val slf4jVersion = "1.7.21"
val configVersion = "1.1.0"
val enumeratumVersion = "1.5.12"
val doobieVersion = "0.4.1"
val cronUtilsVersion = "5.0.5"

val flyway = Seq("org.flywaydb" % "flyway-core" % "4.0.3")

val doobie = Seq(
  "org.tpolecat" %% "doobie-core-cats" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres-cats" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari-cats" % doobieVersion
)

val cats = Seq("org.typelevel" %% "cats" % catsVersion)

val configLib = Seq("com.ccadllc.cedi" %% "config" % configVersion)

val fs2 = Seq(
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-cats" % fs2CatsVersion,
  "co.fs2" %% "fs2-io" % fs2Version
)

val logging = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
)

val enumeratum = Seq("com.beachape" %% "enumeratum" % enumeratumVersion)

val cronUtils = Seq("com.cronutils" % "cron-utils" % cronUtilsVersion)

val scalaCheck = Seq("org.scalacheck" %% "scalacheck" % scalacheckVersion % "test")
val specs2 = Seq(
  "specs2-core",
  "specs2-scalacheck",
  "specs2-mock",
  "specs2-matcher-extra"
).map("org.specs2" %% _ % specs2Version % "test")

val embeddedPostgres = Seq("com.opentable.components" % "otj-pg-embedded" % "0.7.1" % "test")


val deps =
  doobie ++
    flyway ++
    specs2 ++
    embeddedPostgres ++
    scalaCheck ++
    enumeratum ++
    cats ++
    // circe ++
    fs2 ++
    specs2 ++
    cronUtils ++
    logging ++
    configLib

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, GitVersioning)
  .settings(
    scalaVersion := "2.12.2",
    libraryDependencies ++= deps
  )