package com.ingka.warehouse.api.domain

trait Products[F[_]] {
  def findAll: F[Envelope[Products.Product]]

  def create(product: Products.Product): F[Long]
  def read(id: Long): F[Option[Products.Product]]
  def update(product: Products.Product): F[Boolean]
  def delete(id: Long): F[Boolean]

  def sell(id: Long): F[Boolean]
}

object Products {
  final case class Product(id: Long, name: String, articles: List[Article]) {

    val isAvailable = articles.foldLeft(true) {
      case (bool, a) =>
        bool && a.isAvailable
    }

    val inStock: Int =
      if (articles.isEmpty) 0
      else articles.map(a => if (a.amountOf > 0) Math.floorDiv(a.inStock, a.amountOf) else 0).min
  }
  final case class Article(id: Long, amountOf: Int, inStock: Int) {
    val isAvailable: Boolean = amountOf <= inStock
  }
}
