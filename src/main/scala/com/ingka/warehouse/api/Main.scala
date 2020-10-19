package com.ingka.warehouse.api

import java.util.concurrent.Executors

import cats.effect.{Blocker, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import com.ingka.warehouse.api.adapters.db.{InventoryService, ProductsService}
import com.ingka.warehouse.api.adapters.http.{ImportRoutes, ProductsRoutes, Routes}
import com.ingka.warehouse.api.resources.{Configuration, Database, ExecutionManager, Log}
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  private val es = Executors.newWorkStealingPool()
  private val ec = ExecutionContext.fromExecutorService(es)

  override implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  override implicit val timer: Timer[IO]               = IO.timer(ec)

  trait Runtime extends Configuration with ExecutionManager with Log with Database

  val runtime: Resource[IO, Runtime] =
    for {
      l <- Resource.liftF(Slf4jLogger.create[IO])
      _ <- Resource.make(l.info("Starting Warehouse API..."))(_ => l.info("Bye, bye!"))
      _ <- Resource.make(IO.unit)(_ => IO(es.shutdown()))
      b <- Blocker[IO]
      c <- Resource.liftF(Configuration.loadConfig(b))
      d <- Database(c.db, l, b)
    } yield new Runtime {
      def config: Configuration.Config = c
      def globalEC: ExecutionContext   = ec
      def blocker: Blocker             = b
      def getLogger: IO[Logger[IO]]    = Slf4jLogger.create[IO]
      def xa: Transactor[IO]           = d.xa
    }

  def run(args: List[String]): IO[ExitCode] = runtime.use { r =>
    for {

      products  <- ProductsService(r)
      inventory <- InventoryService(r)

      endpoints <- Routes.combine(
        IO.pure(ProductsRoutes(products)),
        ImportRoutes(r, products, inventory)
      )

      exitCode <- BlazeServerBuilder[IO](r.globalEC)
        .bindHttp(r.config.http.port, r.config.http.host)
        .withHttpApp(endpoints.orNotFound)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    } yield exitCode
  }
}
