package com.ingka.warehouse.api.domain

trait Inventory[F[_]] {
  def create(article: Inventory.Article): F[Long]
  def read(id: Long): F[Option[Inventory.Article]]
  def update(article: Inventory.Article): F[Boolean]
  def delete(id: Long): F[Boolean]
  def delete(article: Inventory.Article): F[Boolean] = delete(article.id)
}

object Inventory {
  final case class Article(id: Long = -1, name: String, inStock: Int) {

    def toProduct(amountOf: Int): Products.Article =
      Products.Article(id, amountOf)
  }
}
