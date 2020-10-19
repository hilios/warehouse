package com.ingka.warehouse.api.adapters.http

import cats.data.OptionT
import cats.effect.IO
import cats.implicits._
import com.ingka.warehouse.api.domain.{Envelope, Products}
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.headers.Location
import org.http4s.circe._

case class ProductsRoutes(products: Products[IO]) extends Routes {

  // JSON encoder/decoders
  implicit def envelopeEncoder[A: Encoder]: Encoder[Envelope[A]] = deriveEncoder[Envelope[A]]

  implicit val articleEncoder: Encoder[Products.Article] =
    Encoder.forProduct3("art_id", "amount_of", "in_stock") { a =>
      (a.id, a.amountOf, a.inStock)
    }

  implicit val productEncoder: Encoder[Products.Product] =
    Encoder.forProduct3("id", "name", "contain_articles") { p =>
      (p.id, p.name, p.articles)
    }

  implicit val articleDecoder: Decoder[Products.Article] =
    Decoder.forProduct2("art_id", "amount_of") { (id: Long, amountOf: Int) =>
      Products.Article(id, amountOf)
    }
  implicit val productDecoder: Decoder[Products.Product] =
    Decoder.forProduct2("name", "contain_articles") { (name: String, articles: List[Products.Article]) =>
      Products.Product(name = name, articles = articles)
    }

  private def handleGetAll: IO[Response[IO]] =
    for {
      p  <- products.findAll
      ok <- Ok(p)
    } yield ok

  private def handleCreate(product: Products.Product): IO[Response[IO]] =
    for {
      id      <- products.create(product)
      loc     <- IO.fromEither(Location.parse(s"/products/$id"))
      created <- Created()
    } yield created.withHeaders(loc)

  private def handleGet(id: Long): IO[Response[IO]] = {
    for {
      p <- OptionT(products.read(id))
    } yield p
  }.foldF(NotFound())(Ok(_))

  private def handleUpdate(id: Long)(product: Products.Product): IO[Response[IO]] = {
    for {
      p <- OptionT(products.read(id))
      up = p.copy(name = product.name, articles = product.articles)
      _ <- OptionT.liftF(products.update(up))
    } yield ()
  }.foldF(NotFound())(_ => NoContent())

  private def handleDelete(id: Long): IO[Response[IO]] = {
    for {
      p <- OptionT(products.read(id))
      _ <- OptionT.liftF(products.delete(p.id))
    } yield ()
  }.foldF(NotFound())(_ => NoContent())

  private def handleBuy(id: Long): IO[Response[IO]] = {
    for {
      p <- OptionT(products.read(id))
    } yield ()
  }.foldF(NotFound())(_ => NoContent())

  def routes: HttpRoutes[IO] =
    HttpRoutes
      .of[IO] {
        case GET    -> Root / "products"               => handleGetAll
        case GET    -> Root / "products" / LongVar(id) => handleGet(id)
        case DELETE -> Root / "products" / LongVar(id) => handleDelete(id)
        case request @ POST -> Root / "products" =>
          request.as[Products.Product] >>= handleCreate
        case request @ PUT -> Root / "products" / LongVar(id) =>
          request.as[Products.Product] >>= handleUpdate(id)
        case POST -> Root / "products" / LongVar(id) / "buy" => handleBuy(id)
      }
      .handleErrorWith {
        case _: InvalidMessageBodyFailure   => HttpRoutes.liftF(OptionT.liftF(BadRequest()))
        case _: MalformedMessageBodyFailure => HttpRoutes.liftF(OptionT.liftF(BadRequest()))
      }
}
