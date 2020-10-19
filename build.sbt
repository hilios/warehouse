import sbt._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.ingka"
ThisBuild / organizationName := "INGKA"

lazy val warehouse = (project in file("."))
  .aggregate(api, cli)

lazy val api = (project in file("api"))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(
    name := "warehouse-api",
    libraryDependencies ++= Dependencies.api
  )

lazy val cli = (project in file("cli"))
  .settings(commonSettings)
  .settings(
    name := "warehouse-cli",
    libraryDependencies ++= Dependencies.cli
  )

lazy val commonSettings = Seq(
  autoCompilerPlugins                  := true,
  fork in Test                         := true,
  fork in IntegrationTest              := true,
  parallelExecution in Test            := false,
  parallelExecution in IntegrationTest := false
)
