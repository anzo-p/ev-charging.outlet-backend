package app

import zio.logging.{consoleJson, LogAnnotation, LogFormat}
import zio.{ExitCode, Runtime, Scope, ZIO, ZIOAppDefault, _}

import java.util.UUID

object Main extends ZIOAppDefault {

  private val logger =
    Runtime.removeDefaultLoggers >>> consoleJson(
      LogFormat.default + LogFormat.annotation(LogAnnotation.TraceId)
    )

  def app: ZIO[Any, Nothing, Unit] = for {
    _ <- ZIO.logInfo("App starting")
    _ <- ZIO.succeed(42)
  } yield ()

  override def run: ZIO[Scope, Any, ExitCode] =
    app.exitCode.provide(logger)
}
