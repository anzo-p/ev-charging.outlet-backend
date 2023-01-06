package outlet.backend

import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import outlet.backend.events.{DeviceEndChargingEventConsumer, OutletDeviceMessageConsumer, OutletDeviceMessageProducer}
import outlet.backend.http.OutletRoutes
import outlet.backend.services.DynamoDBChargerOutletService
import shared.events.{ChargingEventProducer, DeadLetterProducer}
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient
import zio.aws.sqs.Sqs
import zio.dynamodb.DynamoDBExecutor

object Main extends ZIOAppDefault {

  val program =
    ZIO
      .serviceWithZIO[OutletRoutes](_.start)
      .zipPar(ZIO.serviceWithZIO[OutletDeviceMessageConsumer](_.start))
      .zipPar(ZIO.serviceWithZIO[DeviceEndChargingEventConsumer](_.start))
      .catchAll {
        case throwable: Throwable => ZIO.succeed(println(throwable.getMessage))
        case _                    => ZIO.succeed(())
      }

  val setup =
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
        DeviceEndChargingEventConsumer.live,
        DynamoDbLeaseRepository.live,
        Kinesis.live,
        ChargingEventProducer.live,
        ChargingEventProducer.make,
        // sqs
        OutletDeviceMessageConsumer.live,
        OutletDeviceMessageProducer.live,
        OutletDeviceMessageProducer.make,
        Sqs.live,
        // http
        OutletRoutes.live,
        // zio
        Scope.default
      )

  override def run: URIO[Any, ExitCode] =
    setup.catchAll {
      case throwable: Throwable => ZIO.succeed(println(throwable.getMessage))
      case _                    => ZIO.succeed(())
    }.exitCode
}
