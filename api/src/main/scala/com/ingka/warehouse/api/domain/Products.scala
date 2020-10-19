package com.ingka.warehouse.api.domain

trait Products[F[_]] {
  def findAll: F[Envelope[Products.Product]]

  def create(product: Products.Product): F[Long]
  def read(id: Long): F[Option[Products.Product]]
  def update(product: Products.Product): F[Boolean]
  def delete(id: Long): F[Boolean]
}

object Products {
  final case class Product(id: Long                              = -1L, name: String, articles: List[Article])
  final case class Article(id: Long, amountOf: Int, inStock: Int = 0) {
    val available: Boolean = amountOf >= inStock
  }
}
