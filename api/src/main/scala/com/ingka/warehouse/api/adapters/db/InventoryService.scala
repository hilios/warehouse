package com.ingka.warehouse.api.adapters.db

import cats.effect.IO
import com.ingka.warehouse.api.domain.Inventory
import doobie._
import doobie.implicits._
import doobie.h2.implicits._
import io.chrisdavenport.log4cats.Logger

case class InventoryService(xa: Transactor[IO], logger: Logger[IO]) extends Inventory[IO] {
  import InventoryService._

  def create(article: Inventory.Article): IO[Long] =
    for {
      id <- insertQuery(article).transact(xa)
      _  <- logger.info(s"Inserted new article [$id]")
    } yield id

  def read(id: Long): IO[Option[Inventory.Article]] =
    for {
      _ <- logger.info(s"Requesting article [$id]")
      a <- selectByIdQuery(id).transact(xa)
    } yield a

  def update(article: Inventory.Article): IO[Boolean] =
    for {
      _ <- logger.info(s"Updating article [${article.id}]")
      n <- updateQuery(article).transact(xa)
    } yield n > 0

  def delete(id: Long): IO[Boolean] =
    for {
      _ <- logger.info(s"Deleting article [${id}]")
      n <- deleteQuery(id).transact(xa)
    } yield n > 0
}

object InventoryService {

  private def insertQuery(article: Inventory.Article): doobie.ConnectionIO[Long] =
    sql"INSERT INTO articles (name, in_stock) VALUES (${article.name}, ${article.inStock})".update
      .withUniqueGeneratedKeys[Long]("id")

  private def selectByIdQuery(id: Long): doobie.ConnectionIO[Option[Inventory.Article]] =
    sql"SELECT id, name, in_stock FROM articles WHERE id = $id".query[Inventory.Article].option

  private def updateQuery(article: Inventory.Article): doobie.ConnectionIO[Int] =
    sql"UPDATE articles SET name = ${article.name}, in_stock = ${article.inStock} WHERE id = ${article.id}".update.run

  private def deleteQuery(id: Long): doobie.ConnectionIO[Int] =
    for {
      n <- sql"DELETE FROM articles WHERE id = $id".update.run
      m <- sql"DELETE FROM products_articles WHERE article_id = $id".update.run
    } yield n + m
}
