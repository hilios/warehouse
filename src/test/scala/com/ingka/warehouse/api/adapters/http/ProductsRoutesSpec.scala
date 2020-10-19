package com.ingka.warehouse.api.adapters.http

import cats.effect.IO
import com.ingka.warehouse.api.domain.{Envelope, Products}
import com.ingka.warehouse.api.test.UnitSpec
import io.chrisdavenport.log4cats.testing.TestingLogger
import io.circe._
import io.circe.literal._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

class ProductsRoutesSpec extends UnitSpec {

  val testLogger = TestingLogger.impl[IO]()
  val products   = mock[Products[IO]]

  val routes = ProductsRoutes(products).routes.orNotFound

  val shelf = Products.Article(
    id       = 1,
    amountOf = 4,
    inStock  = 100
  )

  val product = Products.Product(
    id       = 1,
    name     = "BESTÅ",
    articles = List(shelf)
  )

  "GET /products" should "return a list of all products" in runIO {
    (products.findAll _).expects().returns(IO.pure(Envelope(List(product))))

    val request = Request[IO](Method.GET, uri"/products")
    for {
      response <- routes.apply(request)
      body     <- response.as[Json]
    } yield {
      response.status shouldBe Status.Ok
      body shouldBe
        json"""
          {
            "results": [
              {
                "id": 1,
                "name": "BESTÅ",
                "is_available" : true,
                "contain_articles": [
                  {
                    "art_id": 1,
                    "amount_of": 4,
                    "in_stock": 100
                  }
                ]
              }
            ]
          }
        """
    }
  }

  "GET /products/:id" should "return the product" in runIO {
    (products.read _).expects(1).returns(IO.pure(Some(product)))

    val request = Request[IO](Method.GET, uri"/products/1")
    for {
      response <- routes.apply(request)
      body     <- response.as[Json]
    } yield {
      response.status shouldBe Status.Ok
      body shouldBe
        json"""
          {
            "id" : 1,
            "name" : "BESTÅ",
            "is_available" : true,
            "contain_articles" : [
              {
                "art_id" : 1,
                "amount_of" : 4,
                "in_stock" : 100
              }
            ]
          }
        """
    }
  }

  it should "return not found if no product is returned" in runIO {
    (products.read _).expects(1).returns(IO.pure(None))

    val request = Request[IO](Method.GET, uri"/products/1")
    for {
      response <- routes.apply(request)
    } yield {
      response.status shouldBe Status.NotFound
    }
  }

  "POST /products" should "create a new product" in runIO {
    (products.create _).expects(*).returns(IO.pure(1))

    val request = Request[IO](Method.POST, uri"/products")
      .withEntity(json"""{
          "name": "Foo",
          "contain_articles": [
            {"art_id": 1, "amount_of": 10}
          ]
        }""")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.Created
    }
  }

  it should "return bad request for an invalid payload" in runIO {
    val request = Request[IO](Method.POST, uri"/products")
      .withEntity("""{}""")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.BadRequest
    }
  }

  "PUT /products/:id" should "update a product" in runIO {
    (products.read _).expects(1).returns(IO.pure(Some(product)))
    (products.update _).expects(*).returns(IO.pure(true))

    val request = Request[IO](Method.PUT, uri"/products/1")
      .withEntity(json"""{
          "name": "Bar",
          "contain_articles": [
            {"art_id": 1, "amount_of": 10}
          ]
        }""")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.NoContent
    }
  }

  it should "return not found if no product is returned" in runIO {
    (products.read _).expects(1).returns(IO.pure(None))

    val request = Request[IO](Method.PUT, uri"/products/1")
      .withEntity(json"""{
          "name": "Bar",
          "contain_articles": [
            {"art_id": 1, "amount_of": 10}
          ]
        }""")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.NotFound
    }
  }

  it should "return bad request for an invalid payload" in runIO {
    val request = Request[IO](Method.PUT, uri"/products/1")
      .withEntity(json"""{}""")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.BadRequest
    }
  }

  "DELETE /products/:id" should "delete a product" in runIO {
    (products.read _).expects(1).returns(IO.pure(Some(product)))
    (products.delete _).expects(*).returns(IO.pure(true))

    val request = Request[IO](Method.DELETE, uri"/products/1")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.NoContent
    }
  }

  it should "return not found if no product is returned" in runIO {
    (products.read _).expects(1).returns(IO.pure(None))

    val request = Request[IO](Method.DELETE, uri"/products/1")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.NotFound
    }
  }

  "POST /products/:id/buy" should "execute a sell action" in runIO {
    (products.read _).expects(1).returns(IO.pure(Some(product)))
    (products.sell _).expects(1).returns(IO.pure(true))

    val request = Request[IO](Method.POST, uri"/products/1/buy")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.NoContent
    }
  }

  it should "return a bad request if the sell could not be complete" in runIO {
    (products.read _).expects(1).returns(IO.pure(Some(product)))
    (products.sell _).expects(1).returns(IO.pure(false))

    val request = Request[IO](Method.POST, uri"/products/1/buy")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.BadRequest
    }
  }

  it should "return not found if no product is returned" in runIO {
    (products.read _).expects(1).returns(IO.pure(None))

    val request = Request[IO](Method.POST, uri"/products/1/buy")

    for {
      response <- routes.run(request)
    } yield {
      response.status shouldBe Status.NotFound
    }
  }
}
