import sbt._

object Dependencies {
  lazy val AllTests = s"$Test,$IntegrationTest"

  lazy val version = new {
    val catsCore   = "2.2.0"
    val catsEffect = "2.2.0"
    val circe      = "0.13.0"
    val doobie     = "0.9.2"
    val flyway     = "6.5.3"
    val http4s     = "0.21.7"
    val log4cats   = "1.1.1"
    val logback    = "1.2.3"
    val pureconfig = "0.13.0"
    val scalaTest  = "3.2.2"
    val scalaMock  = "4.4.0"
  }

  lazy val common = Seq(
    "org.typelevel"         %% "cats-core"              % version.catsCore,
    "org.typelevel"         %% "cats-effect"            % version.catsEffect,
    "com.github.pureconfig" %% "pureconfig"             % version.pureconfig,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % version.pureconfig,
    "io.circe"              %% "circe-core"             % version.circe,
    "io.circe"              %% "circe-generic"          % version.circe,
    "io.circe"              %% "circe-parser"           % version.circe,
    "io.circe"              %% "circe-literal"          % version.circe,
    "io.chrisdavenport"     %% "log4cats-core"          % version.log4cats,
    "io.chrisdavenport"     %% "log4cats-slf4j"         % version.log4cats,
    "io.chrisdavenport"     %% "log4cats-testing"       % version.log4cats % AllTests,
    "org.scalatest"         %% "scalatest"              % version.scalaTest % AllTests,
    "org.scalamock"         %% "scalamock"              % version.scalaMock % AllTests
  )

  lazy val api = common ++ Seq(
    "org.http4s"     %% "http4s-blaze-server" % version.http4s,
    "org.http4s"     %% "http4s-circe"        % version.http4s,
    "org.http4s"     %% "http4s-dsl"          % version.http4s,
    "org.tpolecat"   %% "doobie-core"         % version.doobie,
    "org.tpolecat"   %% "doobie-h2"           % version.doobie,
    "org.tpolecat"   %% "doobie-hikari"       % version.doobie,
    "org.flywaydb"   % "flyway-core"          % version.flyway,
    "ch.qos.logback" % "logback-classic"      % version.logback
  )

  lazy val cli = common ++ Seq(
    "org.http4s" %% "http4s-blaze-client" % version.http4s
  )
}
