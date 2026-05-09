ThisBuild / organization := "com.voltionne"
ThisBuild / version := "1.0.1"
ThisBuild / scalaVersion := "3.3.7"


ThisBuild / organizationName := "Voltionne (TM)"


lazy val core = (project in file("core"))
  .settings(
    name := "UniversalApplicationAssembler",
    libraryDependencies ++= Seq(
      "org.snakeyaml" % "snakeyaml-engine" % "3.0.1",
      "org.scalameta" %% "munit" % "1.3.0" % Test)
  )
