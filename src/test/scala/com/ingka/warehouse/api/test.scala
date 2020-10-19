package com.ingka.warehouse.api

import cats.effect.{Blocker, IO, Resource}
import com.ingka.warehouse.api.resources.{Configuration, Database}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

object test {

  trait UnitSpec extends AnyFlatSpec with Matchers with MockFactory {
    def runIO[A](test: IO[A]): A = test.unsafeRunSync()
  }

  trait WithContextShift {
    implicit val contextShift = IO.contextShift(ExecutionContext.global)
    implicit val timer        = IO.timer(ExecutionContext.global)
    val blocker               = Blocker.liftExecutionContext(ExecutionContext.global)
  }

  trait DatabaseSpec extends WithContextShift { self: UnitSpec =>
    val testLogger = Slf4jLogger.create[IO].unsafeRunSync()

    def databaseResource: Resource[IO, Database] = {
      for {
        cfg <- Resource.liftF(Configuration.loadConfig(blocker))
        db  <- Database(cfg.db, testLogger, blocker)
        _   <- Resource.make(db.waitUntilReady)(_ => db.flyway.flatMap(f => IO(f.clean())))
      } yield db
    }
  }
}
