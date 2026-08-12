ThisBuild / organization := "com.voltionne"
ThisBuild / version := "1.1.0"
ThisBuild / scalaVersion := "3.8.4"

ThisBuild / organizationName := "Voltionne (TM)"

lazy val root = (project in file("."))
  .dependsOn(core)
  .settings(
    name := "universal-application-assembler"
  )

lazy val core = (project in file("core"))
  .settings(
    name := "UniversalApplicationAssembler",
    libraryDependencies ++= Seq(
      "org.snakeyaml" % "snakeyaml-engine" % "3.0.1",
      "org.scalameta" %% "munit" % "1.3.0" % Test)
  )
