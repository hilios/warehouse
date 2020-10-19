package com.ingka.warehouse.api.adapters.db

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits._
import com.ingka.warehouse.api.domain.{Envelope, Products}
import com.ingka.warehouse.api.resources.{Database, Log}
import doobie._
import doobie.implicits._
import org.h2.api.ErrorCode
import io.chrisdavenport.log4cats.Logger

case class ProductsService(xa: Transactor[IO], logger: Logger[IO]) extends Products[IO] {
  import ProductsService._

  def findAll: IO[Envelope[Products.Product]] =
    for {
      results <- selectAll.transact(xa)
    } yield Envelope(results)

  def create(product: Products.Product): IO[Long] =
    for {
      id <- insertQuery(product).transact(xa)
      _  <- logger.info(s"Inserted new product [$id]")
    } yield id

  def read(id: Long): IO[Option[Products.Product]] =
    for {
      _ <- logger.info(s"Requesting product [$id]")
      p <- selectByIdQuery(id).transact(xa)
    } yield p

  def update(product: Products.Product): IO[Boolean] =
    for {
      _ <- logger.info(s"Updating product [${product.id}]")
      n <- updateQuery(product).transact(xa)
    } yield n > 0

  def delete(id: Long): IO[Boolean] =
    for {
      _ <- logger.info(s"Deleting product [$id]")
      n <- deleteQuery(id).transact(xa)
    } yield n > 0

  def sell(id: Long): IO[Boolean] =
    for {
      _ <- logger.info(s"Updating product [$id] inventory")
      n <- sellQuery(id).transact(xa).exceptSomeSqlState {
        case CHECK_CONSTRAINT_VIOLATED => IO.pure(0)
      }
    } yield n > 0
}

object ProductsService {

  def apply(runtime: Database with Log): IO[ProductsService] =
    for {
      logger <- runtime.getLogger
    } yield ProductsService(runtime.xa, logger)

  private val CHECK_CONSTRAINT_VIOLATED = SqlState(ErrorCode.CHECK_CONSTRAINT_VIOLATED_1.toString)

  private def deleteProductArticles(productId: Long): doobie.Update0 =
    sql"DELETE FROM products_articles WHERE product_id = $productId".update

  private def insertManyArticlesQuery(productId: Long, articles: List[Products.Article]) =
    Update[(Long, Long, Int)](
      "INSERT INTO products_articles (product_id, article_id, amount_of) VALUES (?, ?, ?)"
    ).updateMany(articles.map(a => (productId, a.id, a.amountOf)))

  private def insertQuery(product: Products.Product) =
    for {
      id <- sql"INSERT INTO products (name) VALUES (${product.name})".update
        .withUniqueGeneratedKeys[Long]("id")
      _ <- insertManyArticlesQuery(id, product.articles)
    } yield id

  // TODO: Add pagination
  private val selectAll: doobie.ConnectionIO[List[Products.Product]] =
    for {
      p <- sql"SELECT id, name FROM products".query[(Long, String)].to[List]
      ids = p.map(_._1).toNel.getOrElse(NonEmptyList.one(-1L))
      a <- sql"""
           SELECT product_id, article_id, amount_of, in_stock 
           FROM products_articles 
           INNER JOIN articles ON articles.id = products_articles.article_id
           WHERE ${Fragments.in(fr"product_id", ids)}"""
        .query[(Long, Products.Article)]
        .to[List]
      articles = a.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
    } yield p.map {
      case (id, name) =>
        Products.Product(id, name, articles.getOrElse(id, List.empty))
    }

  private def selectArticlesByProductId(id: Long): doobie.ConnectionIO[List[Products.Article]] =
    sql"""
    SELECT article_id, amount_of, in_stock
    FROM products_articles
    INNER JOIN articles ON articles.id = products_articles.article_id
    WHERE product_id = $id"""
      .query[Products.Article]
      .to[List]

  private def selectByIdQuery(id: Long): doobie.ConnectionIO[Option[Products.Product]] =
    for {
      a <- selectArticlesByProductId(id)
      p <- sql"SELECT id, name FROM products WHERE id = $id"
        .query[(Long, String)]
        .map {
          case (id, name) =>
            Products.Product(id, name, a)
        }
        .option
    } yield p

  private def updateQuery(product: Products.Product): doobie.ConnectionIO[Int] =
    for {
      n <- sql"UPDATE products SET name = ${product.name} WHERE id = ${product.id}".update.run
      _ <- deleteProductArticles(product.id).run
      m <- insertManyArticlesQuery(product.id, product.articles)
    } yield n + m

  private def deleteQuery(id: Long): doobie.ConnectionIO[Int] =
    for {
      m <- deleteProductArticles(id).run
      n <- sql"DELETE FROM products WHERE id = $id".update.run
    } yield n + m

  // Everything happens inside the same transaction
  private def sellQuery(id: Long): doobie.ConnectionIO[Int] =
    for {
      articles <- selectArticlesByProductId(id)
      updated = articles.map(a => (a.inStock - a.amountOf) -> a.id)
      n <- Update[(Int, Long)](
        "UPDATE articles SET in_stock = ? WHERE id = ?"
      ).updateMany(updated)
    } yield n
}
