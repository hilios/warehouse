package com.ingka.warehouse.api.domain

trait Products[F[_]] {
  def create(product: Products.Product): F[Long]
  def read(id: Long): F[Option[Products.Product]]
  def update(product: Products.Product): F[Boolean]
  def delete(id: Long): F[Boolean]
  def delete(product: Products.Product): F[Boolean] = delete(product.id)
}

object Products {
  final case class Product(id: Long = -1L, name: String, articles: List[Article])
  final case class Article(inventoryId: Long, amountOf: Int)
}
