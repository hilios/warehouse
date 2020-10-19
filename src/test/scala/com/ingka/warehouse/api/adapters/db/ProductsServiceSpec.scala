package com.ingka.warehouse.api.adapters.db

import cats.effect.IO
import com.ingka.warehouse.api.domain.{Envelope, Inventory, Products}
import com.ingka.warehouse.api.test.{DatabaseSpec, UnitSpec}

// Actually a integration spec, it's here for the sake of simplicity
class ProductsServiceSpec extends UnitSpec with DatabaseSpec {

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

  it should "support basic CRUD operations" in withServices { (inventory, products) =>
    for {
      aid <- inventory.create(article)
      a1  <- inventory.read(aid).flatMap(IO.fromOption(_)(new NoSuchElementException))
      // Create a product with an inventory article
      p0 = product.copy(articles = List(a1.toProduct(5)))
      id <- products.create(p0)
      p1 <- products.read(id)
      pE <- products.findAll
      _  <- products.update(p1.get.copy(name = "Bar"))
      p2 <- products.read(id)
      _  <- products.delete(id)
      p3 <- products.read(id)
    } yield {
      p1 shouldBe Some(p0.copy(id = id))
      pE shouldBe Envelope(List(p1.get))
      p2.map(_.name) shouldBe Some("Bar")
      p3 shouldBe None
    }
  }

  "#sell(id)" should "update the articles inventory for the product" in withServices { (inventory, products) =>
    for {
      aid <- inventory.create(article.copy(inStock = 6))
      a1  <- inventory.read(aid).flatMap(IO.fromOption(_)(new NoSuchElementException))
      p0 = product.copy(articles = List(a1.toProduct(5)))
      id <- products.create(p0)
      p1 <- products.read(id).flatMap(IO.fromOption(_)(new NoSuchElementException))
      s1 <- products.sell(id)
      a2 <- inventory.read(aid).flatMap(IO.fromOption(_)(new NoSuchElementException))
      // Try to sell without in stock inventory
      s2 <- products.sell(id)
      a3 <- inventory.read(aid).flatMap(IO.fromOption(_)(new NoSuchElementException))
    } yield {
      p1.isAvailable shouldBe true
      s1 shouldBe true
      a2.inStock shouldBe 1
      s2 shouldBe false
      a3.inStock shouldBe 1
    }
  }
}
