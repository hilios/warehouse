package com.ingka.warehouse.api.resources

import cats.effect.IO
import io.chrisdavenport.log4cats.Logger

trait Log {
  def getLogger: IO[Logger[IO]]
}
