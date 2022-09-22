package app

import zio._
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  private val logger =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  def app: ZIO[Any, Nothing, Unit] = for {
    _ <- ZIO.logInfo("App starting")
    _ <- ZIO.succeed(42)
  } yield ()

  override def run: ZIO[Scope, Any, ExitCode] =
    app.exitCode.provide(logger)
}
