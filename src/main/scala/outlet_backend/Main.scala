package outlet_backend

import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import outlet_backend.events.{KinesisChargingEventsIn, SQSOutletDeviceMessagesIn, SQSOutletDeviceMessagesOut}
import outlet_backend.http.OutletRoutes
import outlet_backend.services.DynamoDBChargerOutletService
import shared.events.kinesis.{KinesisChargingEventsOut, KinesisDeadLetters}
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
      .zipPar(ZIO.serviceWithZIO[SQSOutletDeviceMessagesIn](_.start))
      .zipPar(ZIO.serviceWithZIO[KinesisChargingEventsIn](_.start))
      .catchAll {
        case throwable: Throwable => ZIO.succeed(println(throwable.getMessage))
        case _                    => ZIO.succeed(())
      }

  val setup =
    program
      .provide(
        // aws config
        AwsAccessConfig.live,
        AwsConfig.configured(),
        NettyHttpClient.default,
        // dynamodb
        DynamoDb.live,
        DynamoDBChargerOutletService.live,
        DynamoDBExecutor.live,
        // kinesis
        DynamoDbLeaseRepository.live,
        Kinesis.live,
        KinesisChargingEventsIn.live,
        KinesisChargingEventsOut.live,
        KinesisChargingEventsOut.make,
        KinesisDeadLetters.live,
        KinesisDeadLetters.make,
        // sqs
        SQSOutletDeviceMessagesIn.live,
        SQSOutletDeviceMessagesOut.live,
        SQSOutletDeviceMessagesOut.make,
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
