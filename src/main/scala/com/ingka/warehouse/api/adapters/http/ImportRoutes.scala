package com.ingka.warehouse.api.adapters.http

import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.ingka.warehouse.api.domain.{Envelope, Inventory, Products}
import com.ingka.warehouse.api.resources.Log
import io.chrisdavenport.log4cats.Logger
import io.circe.Decoder
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{HttpRoutes, Request, Response}

case class ImportRoutes(logger: Logger[IO], products: Products[IO], inventory: Inventory[IO])(
  implicit CS: ContextShift[IO]
) extends Routes {

  // JSON decoders
  implicit lazy val inventoryDecoder: Decoder[Inventory.Article] =
    Decoder.forProduct3("art_id", "name", "stock") { (id: Long, name: String, inStock: Int) =>
      Inventory.Article(id, name, inStock)
    }

  implicit lazy val articleDecoder: Decoder[Products.Article] =
    Decoder.forProduct2("art_id", "amount_of") { (id: Long, amountOf: Int) =>
      Products.Article(id, amountOf, inStock = 0)
    }

  implicit lazy val productDecoder: Decoder[Products.Product] =
    Decoder.forProduct2("name", "contain_articles") { (name: String, articles: List[Products.Article]) =>
      Products.Product(id = -1, name = name, articles = articles)
    }

  implicit lazy val productsEnvelopeDecoder: Decoder[Envelope[Products.Product]] =
    Decoder.forProduct1("products") { results: List[Products.Product] =>
      Envelope(results)
    }

  implicit lazy val inventoryEnvelopeDecoder: Decoder[Envelope[Inventory.Article]] =
    Decoder.forProduct1("inventory") { results: List[Inventory.Article] =>
      Envelope(results)
    }

  // Import actions
  private def handleProducts(part: Part[IO]): IO[Unit] = {
    for {
      e <- part.as[Envelope[Products.Product]]
      _ <- e.results.traverse(products.create)
      _ <- logger.info(s"Products loaded from [${part.filename.getOrElse("-")}]")
    } yield ()
  }.handleErrorWith {
    case e => logger.error(e)("Could not process products")
  }

  private def handleArticles(part: Part[IO]): IO[Unit] = {
    for {
      e <- part.as[Envelope[Inventory.Article]]
      _ <- e.results.traverse(inventory.create)
      _ <- logger.info(s"Inventory loaded from [${part.filename.getOrElse("-")}]")
    } yield ()
  }.handleErrorWith {
    case e => logger.error(e)("Could not process inventory")
  }

  // HTTP handlers
  private def handleImport[A](request: Request[IO])(fn: Part[IO] => IO[A]): IO[Response[IO]] =
    request.decode[Multipart[IO]] { data =>
      data.parts.headOption match {
        case Some(f) => Accepted() <* fn(f).start
        case None    => BadRequest()
      }
    }

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ POST -> Root / "inventory" / "import" => handleImport(request)(handleArticles)
    case request @ POST -> Root / "products" / "import"  => handleImport(request)(handleProducts)
  }
}

object ImportRoutes {

  def apply(log: Log, products: Products[IO], inventory: Inventory[IO])(
    implicit CS: ContextShift[IO]
  ): IO[ImportRoutes] =
    for {
      logger <- log.getLogger
    } yield ImportRoutes(logger, products, inventory)
}
