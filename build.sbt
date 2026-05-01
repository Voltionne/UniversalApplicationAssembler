ThisBuild / version := "0.2.0"

ThisBuild / scalaVersion := "3.3.7"

lazy val core = (project in file("core"))
  .settings(
    name := "UniversalApplicationAssembler",
    libraryDependencies ++= Seq(
      "org.yaml" % "snakeyaml" % "2.6",
      "org.scalameta" %% "munit" % "1.3.0" % Test)
  )
