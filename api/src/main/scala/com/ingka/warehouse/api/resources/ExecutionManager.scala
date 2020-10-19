package com.ingka.warehouse.api.resources

import cats.effect.Blocker

import scala.concurrent.ExecutionContext

trait ExecutionManager {
  def globalEC: ExecutionContext
  def blocker: Blocker
}

object ExecutionManager {

  def apply(ec: ExecutionContext, blk: Blocker): ExecutionManager = new ExecutionManager {
    def globalEC: ExecutionContext = ec
    def blocker: Blocker           = blk
  }
}
