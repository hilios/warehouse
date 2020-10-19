package com.ingka.warehouse.api.resources

import cats.effect.{Blocker, ContextShift, IO}
import pureconfig._
import pureconfig.module.catseffect.syntax._
import pureconfig.generic.auto._

trait Configuration {
  def config: Configuration.Config
}

object Configuration {
  final case class Config(http: HttpConfig, db: DatabaseConfig)
  final case class HttpConfig(host: String, port: Int)
  final case class DatabaseConfig(url: String, username: String, password: String, connectionPollSize: Int)

  def loadConfig(blocker: Blocker)(implicit CS: ContextShift[IO]): IO[Config] =
    ConfigSource.default.at("com.ingka.warehouse.api").loadF[IO, Config](blocker)
}
