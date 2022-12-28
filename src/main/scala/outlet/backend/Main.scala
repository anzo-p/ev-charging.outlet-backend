package outlet.backend

import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import outlet.backend.events.{OutletEventConsumer, OutletStatusProducer}
import outlet.backend.http.OutletRoutes
import outlet.backend.services.DynamoDBChargerOutletService
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient
import zio.dynamodb.DynamoDBExecutor

object Main extends ZIOAppDefault {

  val program =
    ZIO.serviceWithZIO[OutletRoutes](_.start).zipPar(ZIO.serviceWithZIO[OutletEventConsumer](_.start))

  override def run: ZIO[Any, Throwable, ExitCode] =
    program.provide(
      AwsConfig.default,
      DynamoDb.live,
      DynamoDBChargerOutletService.live,
      DynamoDBExecutor.live,
      DynamoDbLeaseRepository.live,
      Kinesis.live,
      NettyHttpClient.default,
      OutletEventConsumer.live,
      OutletRoutes.live,
      OutletStatusProducer.live,
      OutletStatusProducer.make,
      Scope.default
    )
}
