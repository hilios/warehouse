package com.ingka.warehouse.api.adapters.http

import cats.effect.IO
import com.ingka.warehouse.api.domain.{Inventory, Products}
import com.ingka.warehouse.api.test.{UnitSpec, WithContextShift}
import io.chrisdavenport.log4cats.testing.TestingLogger._
import io.chrisdavenport.log4cats.testing.TestingLogger
import org.http4s._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.http4s.multipart.{Multipart, Part}
import org.scalatest.concurrent.Eventually

import scala.util.Random

class ImportRoutesSpec extends UnitSpec with WithContextShift with Eventually {

  val testLogger = TestingLogger.impl[IO]()
  val products   = mock[Products[IO]]
  val inventory  = mock[Inventory[IO]]

  val routes = ImportRoutes(testLogger, products, inventory).routes.orNotFound

  "POST /products/import" should "import a JSON file" in runIO {
    (products.create _).expects(*).returns(IO.pure(Random.nextLong())).anyNumberOfTimes()

    val json = getClass.getResource("/products.json")
    val part = Part.fileData[IO](json.getFile, json, blocker, `Content-Type`(MediaType.application.json))
    val body = Multipart[IO](Vector(part))

    val request = Request[IO](Method.POST, uri"/products/import").withEntity(body).withHeaders(body.headers)

    for {
      response <- routes.run(request)
    } yield eventually {
      response.status shouldBe Status.Accepted
      testLogger.logged.unsafeRunSync() should contain(
        INFO("Products loaded from [products.json]", None)
      )
    }
  }

  "POST /inventory/import" should "import a JSON file" in runIO {
    (inventory.create _).expects(*).returns(IO.pure(Random.nextLong())).anyNumberOfTimes()

    val json = getClass.getResource("/inventory.json")
    val part = Part.fileData[IO](json.getFile, json, blocker, `Content-Type`(MediaType.application.json))
    val body = Multipart[IO](Vector(part))

    val request = Request[IO](Method.POST, uri"/inventory/import").withEntity(body).withHeaders(body.headers)

    for {
      response <- routes.run(request)
    } yield eventually {
      response.status shouldBe Status.Accepted
      testLogger.logged.unsafeRunSync() should contain(
        INFO("Inventory loaded from [inventory.json]", None)
      )
    }
  }
}
