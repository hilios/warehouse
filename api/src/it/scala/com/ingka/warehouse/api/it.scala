package com.ingka.warehouse.api

import cats.effect.{Blocker, IO, Resource}
import com.ingka.warehouse.api.resources.{Configuration, Database}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

object it {

  trait IntegrationSpec extends AnyFlatSpec with Matchers {
    implicit val contextShift = IO.contextShift(ExecutionContext.global)
    implicit val timer        = IO.timer(ExecutionContext.global)
    val blocker               = Blocker.liftExecutionContext(ExecutionContext.global)
    val testLogger            = Slf4jLogger.create[IO].unsafeRunSync()
  }

  trait DatabaseSpec { self: IntegrationSpec =>

    def databaseResource: Resource[IO, Database] = {
      for {
        cfg <- Resource.liftF(Configuration.loadConfig(blocker))
        db  <- Database(cfg.db, testLogger, blocker)
        _   <- Resource.make(db.waitUntilReady)(_ => db.flyway.flatMap(f => IO(f.clean())))
      } yield db
    }
  }
}
