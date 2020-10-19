package com.ingka.warehouse.api

import cats.effect.IO
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

object test {

  trait UnitSpec extends AnyFlatSpec with Matchers with MockFactory {
    def runIO[A](test: IO[A]): A = test.unsafeRunSync()
  }
}
