import sbt._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.ingka"
ThisBuild / organizationName := "INGKA"

lazy val warehouse = (project in file("."))
  .settings(
    name                      := "warehouse",
    autoCompilerPlugins       := true,
    fork in Test              := true,
    parallelExecution in Test := false,
    libraryDependencies ++= Dependencies.libraries
  )
