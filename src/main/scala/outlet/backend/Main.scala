package outlet.backend

import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import outlet.backend.events.{DeviceEndOutletEventConsumer, DeviceEndOutletEventProducer}
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
    ZIO.serviceWithZIO[OutletRoutes](_.start).zipPar(ZIO.serviceWithZIO[DeviceEndOutletEventConsumer](_.start))

  override def run: ZIO[Any, Throwable, ExitCode] =
    program
      .provide(
        AwsConfig.default,
        DynamoDb.live,
        DynamoDBChargerOutletService.live,
        DynamoDBExecutor.live,
        DynamoDbLeaseRepository.live,
        Kinesis.live,
        NettyHttpClient.default,
        DeviceEndOutletEventConsumer.live,
        OutletRoutes.live,
        DeviceEndOutletEventProducer.live,
        DeviceEndOutletEventProducer.make,
        Scope.default
      )
      .exitCode
}
