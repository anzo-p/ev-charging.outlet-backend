package charger.backend

import charger.backend.events.KinesisStreamWriter
import charger.backend.http.ChargerRoutes
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient

object Main extends ZIOAppDefault {

  val program: ZIO[ChargerRoutes, Throwable, Nothing] =
    ZIO.serviceWithZIO[ChargerRoutes](_.start)

  override def run: ZIO[Any, Throwable, Nothing] =
    program.provide(
      NettyHttpClient.default,
      AwsConfig.default,
      Kinesis.live,
      KinesisStreamWriter.live,
      KinesisStreamWriter.make,
      ChargerRoutes.live,
      Scope.default
    )
}
