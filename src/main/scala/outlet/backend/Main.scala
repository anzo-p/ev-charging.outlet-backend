package outlet.backend

import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import outlet.backend.events.DeviceEndOutletEventConsumer
import outlet.backend.http.OutletRoutes
import outlet.backend.services.DynamoDBChargerOutletService
import shared.events.{DeadLetterProducer, OutletEventProducer}
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
        // aws config
        AwsConfig.default,
        NettyHttpClient.default,
        // dynamodb
        DynamoDb.live,
        DynamoDBChargerOutletService.live,
        DynamoDBExecutor.live,
        // kinesis
        DeadLetterProducer.live,
        DeadLetterProducer.make,
        DeviceEndOutletEventConsumer.live,
        DynamoDbLeaseRepository.live,
        Kinesis.live,
        OutletEventProducer.live,
        OutletEventProducer.make,
        // http
        OutletRoutes.live,
        // zio
        Scope.default
      )
      .exitCode
}
