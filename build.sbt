organization := "com.voltionne"
version := "1.3.0"
scalaVersion := "3.8.4"

organizationName := "Voltionne (TM)"

lazy val root = (project in file("."))
  .dependsOn(core)
  .settings(
    name := "universal-application-assembler"
  )

lazy val core = (project in file("core"))
  .settings(
    name := "UniversalApplicationAssembler",
    libraryDependencies ++= Seq(
      "org.snakeyaml" % "snakeyaml-engine" % "3.1.1",
      "org.scalameta" %% "munit" % "1.3.5" % Test)
  )
