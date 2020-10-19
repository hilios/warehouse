package com.ingka.warehouse.api.resources

import cats.effect.concurrent.{Deferred, TryableDeferred}
import cats.effect.{Blocker, Concurrent, ContextShift, IO, Resource}
import doobie.ExecutionContexts
import doobie.h2.H2Transactor
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.Logger
import org.flywaydb.core.Flyway

trait Database {
  def xa: Transactor[IO]
}

object Database {
  type Ready = TryableDeferred[IO, Boolean]

  trait Migrations {
    def flyway: IO[Flyway]
    def isReady: IO[Boolean]
    def waitUntilReady: IO[Unit]
  }

  def flyway(url: String, username: String, password: String): IO[Flyway] =
    for {
      cfg <- IO {
        Flyway
          .configure()
          .dataSource(url, username, password)
      }
      flyway <- IO(new Flyway(cfg))
    } yield flyway

  def apply(cfg: Configuration.DatabaseConfig, logger: Logger[IO], blocker: Blocker)(
    implicit F: Concurrent[IO],
    CS: ContextShift[IO]
  ): Resource[IO, Database with Migrations] =
    for {
      ready <- Resource.liftF(Deferred.tryable[IO, Boolean])
      // Migrations
      f <- Resource.liftF(flyway(cfg.url, cfg.username, cfg.password))
      _ <- Resource.liftF(
        IO(f.migrate())
          .redeemWith(
            e => ready.complete(false) *> logger.error(e)("Migrations failed!"),
            _ => ready.complete(true) *> logger.info("Migrations succeeded!")
          )
          .start
      )
      // Connection poll
      ce <- ExecutionContexts.fixedThreadPool[IO](cfg.connectionPollSize)
      h2 <- H2Transactor.newH2Transactor[IO](
        cfg.url,
        cfg.username,
        cfg.password,
        ce, // await connection here
        blocker // execute JDBC operations here
      )
    } yield new Database with Migrations {
      def xa: Transactor[IO] = h2

      def isReady: IO[Boolean] = ready.tryGet.map(_.getOrElse(false))

      def waitUntilReady: IO[Unit] =
        ready.get.flatMap { status =>
          if (status) IO.unit else IO.raiseError(new RuntimeException("Database migrations failed!"))
        }

      def flyway: IO[Flyway] = IO.pure(f)
    }
}
