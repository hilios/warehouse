package com.ingka.warehouse.cli

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO.raiseError(new NotImplementedError())
}