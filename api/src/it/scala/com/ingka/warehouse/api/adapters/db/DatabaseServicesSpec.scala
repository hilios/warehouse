package com.ingka.warehouse.api.adapters.db

import cats.effect.IO
import com.ingka.warehouse.api.domain.{Inventory, Products}
import com.ingka.warehouse.api.it.{DatabaseSpec, IntegrationSpec}

class DatabaseServicesSpec extends IntegrationSpec with DatabaseSpec {

  def withServices[A](test: (Inventory[IO], Products[IO]) => IO[A]): A = {
    for {
      db <- databaseResource
      i = InventoryService(db.xa, testLogger)
      p = ProductsService(db.xa, testLogger)
    } yield (i, p)
  }.use(test.tupled).unsafeRunSync()

  val article = Inventory.Article(
    name    = "Shelf",
    inStock = 1
  )

  val product = Products.Product(
    name     = "BESTÅ",
    articles = List.empty
  )

  behavior of "InventoryService"

  it should "support basic CRUD operations" in withServices { (inventory, _) =>
    for {
      id <- inventory.create(article)
      a1 <- inventory.read(id)
      a0 = article.copy(id = id)
      _  <- inventory.update(a0.copy(name = "Foo"))
      a2 <- inventory.read(id)
      _  <- inventory.delete(id)
      a3 <- inventory.read(id)
    } yield {
      a1 shouldBe Some(a0)
      a2.map(_.name) shouldBe Some("Foo")
      a3 shouldBe None
    }
  }

  behavior of "ProductsService"

  it should "support basic CRUD operations" in withServices { (inventory, products) =>
    for {
      aid <- inventory.create(article)
      a1  <- inventory.read(aid).flatMap(IO.fromOption(_)(new NoSuchElementException))
      // Create a product with an inventory article
      p = product.copy(articles = List(a1.toProduct(5)))
      id <- products.create(p)
      p1 <- products.read(id)
      p0 = p.copy(id = id)
      _  <- products.update(p0.copy(name = "Bar"))
      p2 <- products.read(id)
      _  <- products.delete(id)
      p3 <- products.read(id)
    } yield {
      p1 shouldBe Some(p0)
      p2.map(_.name) shouldBe Some("Bar")
      p3 shouldBe None
    }
  }
}
