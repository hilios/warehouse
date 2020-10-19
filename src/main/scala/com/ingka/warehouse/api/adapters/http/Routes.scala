package com.ingka.warehouse.api.adapters.http

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.circe.{CirceEntityDecoder, CirceEntityEncoder}
import org.http4s.dsl.Http4sDsl

trait Routes extends Http4sDsl[IO] with CirceEntityEncoder with CirceEntityDecoder {
  def routes: HttpRoutes[IO]
}
