package com.ingka.warehouse.api.adapters.db

import cats.effect.IO
import com.ingka.warehouse.api.domain.{Envelope, Inventory, Products}
import com.ingka.warehouse.api.test.{DatabaseSpec, UnitSpec}

// Actually a integration spec, it's here for the sake of simplicity
class InventoryServiceSpec extends UnitSpec with DatabaseSpec {

  def withServices[A](test: (Inventory[IO], Products[IO]) => IO[A]): A = {
    for {
      db <- databaseResource
      i = InventoryService(db.xa, testLogger)
      p = ProductsService(db.xa, testLogger)
    } yield (i, p)
  }.use(test.tupled).unsafeRunSync()

  val article = Inventory.Article(
    id      = -1,
    name    = "Shelf",
    inStock = 10
  )

  val product = Products.Product(
    id       = -1,
    name     = "BESTÅ",
    articles = List.empty
  )

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

  it should "check the inventory for a specific product" in withServices { (inventory, products) =>
    for {
      a1Id <- inventory.create(article)
      a1   <- inventory.read(a1Id).flatMap(IO.fromOption(_)(new NoSuchElementException))
      a2Id <- inventory.create(article.copy(name = "Screws", inStock = 10))
      a2   <- inventory.read(a2Id).flatMap(IO.fromOption(_)(new NoSuchElementException))
      // Create a product with an inventory article
      p = product.copy(articles = List(a1.toProduct(5)))
      pid <- products.create(p)
      aE  <- inventory.findAll
      aP  <- inventory.findByProductId(pid)
    } yield {
      aE shouldBe Envelope(List(a1, a2))
      aP shouldBe Envelope(List(a1))
    }
  }
}
