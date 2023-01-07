package outlet.backend

import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import outlet.backend.events.{KinesisChargingEventsIn, SQSOutletDeviceMessagesIn, SQSOutletDeviceMessagesOut}
import outlet.backend.http.OutletRoutes
import outlet.backend.services.DynamoDBChargerOutletService
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
        AwsConfig.default,
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
/*
  outlet has something to say - from SQS consumer
  - ok cable plugged
    - update outlet - dynamodb

  - ok charging requested
    - verify outlet - dynamodb
    - forward to app - kinesis

  - ok currently charging and intermediate consumption report
    - update outlet - device state and totals - dynamodb
    - forward to app - kinesis

  - ok stopping requested with final consumption report
    - update outlet - device state and totals - dynamodb
    - forward to app - kinesis

  - ok cable unplugged, ie available
    - update outlet - dynamodb

  app has something top say - from Kinesis consumer
  - charging requested by app - customership verified, session started
    - update outlet - dynamodb
    - forward to device - sqs
    (app will be acked by next status report)

  - charging approved by app - customership verified, session started
    - update outlet - dynamodb
    - forward to device - sqs

  - stop charging requested by app
    - update outlet - dynamodb
    - forward to device - sqs
    - produce totals report to app - kinesis
 */

/*
  outlet backend has something to say - always kinesis
  - charging requested by outlet
    - verify customership - dynamodb
    - initiate charging session - dynamodb
    - ack back to app - kinesis
    - nack back if validations not ok

  - charging report from outlet
    - update charging session - dynamodb

  - stopped by outlet with final report
    - update chartging session - dynamodb
 */
