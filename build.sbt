ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "UniversalApplicationCompiler",
    libraryDependencies += "org.yaml" % "snakeyaml" % "2.5"   // latest stable version
  )
