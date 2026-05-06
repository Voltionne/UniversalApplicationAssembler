ThisBuild / version := "0.2.0"

ThisBuild / organizationName := "com.voltionne"

ThisBuild / scalaVersion := "3.3.7"

lazy val core = (project in file("core"))
  .settings(
    name := "UniversalApplicationAssembler",
    libraryDependencies ++= Seq(
      "org.snakeyaml" % "snakeyaml-engine" % "3.0.1",
      "org.scalameta" %% "munit" % "1.3.0" % Test)
  )
