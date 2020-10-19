package com.ingka.warehouse.api.domain

final case class Envelope[A](results: List[A])

object Envelope {
  def empty[A]: Envelope[A] = Envelope(results = List.empty)
}
