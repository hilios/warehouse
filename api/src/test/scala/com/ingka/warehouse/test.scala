package com.ingka.warehouse

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

object test {
  trait UnitSpec extends AnyFlatSpec with Matchers with MockFactory
}
