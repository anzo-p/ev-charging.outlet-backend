package charger.backend

import charger.backend.events.ChargingSessionConsumer
import nl.vroste.zio.kinesis.client.zionative.LeaseRepository
import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient

object Main extends ZIOAppDefault {

  val program: ZIO[Kinesis with LeaseRepository with Any, Throwable, Unit] =
    ChargingSessionConsumer.read

  override def run: ZIO[Any, Throwable, Unit] =
    program.provide(
      AwsConfig.default,
      DynamoDb.live,
      DynamoDbLeaseRepository.live,
      Kinesis.live,
      NettyHttpClient.default
    )
}
