ThisBuild / version := "0.2.0"

ThisBuild / scalaVersion := "3.3.7"

Compile / scalaSource := baseDirectory.value / "src"

lazy val root = (project in file("."))
  .settings(
    name := "UniversalApplicationCompiler",
    libraryDependencies += "org.yaml" % "snakeyaml" % "2.6"   // latest stable version
  )
