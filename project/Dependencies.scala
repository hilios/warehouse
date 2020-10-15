import sbt._

object Dependencies {
  lazy val versions = new {
    val http4s = "0.21.7"
    val catsCore   = "2.2.0"
    val catsEffect = "2.2.0"
    val scalaTest = "3.2.2"
  }

  lazy val common = Seq(
    "org.typelevel" %% "cats-core"   % versions.catsCore,
    "org.typelevel" %% "cats-effect" % versions.catsEffect,
    "org.scalatest" %% "scalatest"   % versions.scalaTest % Test
  )

  lazy val api = common ++ Seq(
    "org.http4s" %% "http4s-blaze-server" % versions.http4s,
    "org.http4s" %% "http4s-circe"        % versions.http4s,
    "org.http4s" %% "http4s-dsl"          % versions.http4s
  )

  lazy val cli = common ++ Seq(
    "org.http4s" %% "http4s-blaze-client" % versions.http4s
  )
}
