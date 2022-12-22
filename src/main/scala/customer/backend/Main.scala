package customer.backend

import customer.backend.events.{ChargingSessionProducer, OutletEventConsumer}
import customer.backend.http.{ChargingRequestRoutes, CustomerRoutes, CustomerServer}
import customer.backend.service.{DynamoDBChargingService, DynamoDBCustomerService}
import nl.vroste.zio.kinesis.client.zionative.LeaseRepository
import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient
import zio.dynamodb.DynamoDBExecutor

object Main extends ZIOAppDefault {

  val program: ZIO[Kinesis with LeaseRepository with Any with CustomerServer, Throwable, Unit] =
    ZIO.serviceWithZIO[CustomerServer](_.start) *> OutletEventConsumer.read

  override def run: URIO[Any, ExitCode] =
    program
      .provide(
        AwsConfig.default,
        ChargingSessionProducer.make,
        ChargingSessionProducer.live,
        ChargingRequestRoutes.live,
        CustomerRoutes.live,
        CustomerServer.live,
        DynamoDb.live,
        DynamoDBChargingService.live,
        DynamoDBCustomerService.live,
        DynamoDBExecutor.live,
        DynamoDbLeaseRepository.live,
        Kinesis.live,
        NettyHttpClient.default,
        Scope.default
      )
      .exitCode
}

/*
  sbt run -jvm-debug 9999

  Customer requests charging session
  - charger stores into dynamodb
  - charger pushes to device - just log for now
  - charger emits what ??? to kinesis
  - consumer reads that
  - consumer updates things in dynamodb
  - client polls respective endpoint ??? where consumer can see that the charging event eventually begun


  Consumer client initiates

  - initiate charging
             consumer client  -> consumer backend -> post - start / stop charging for consumer at device
             consumer backend -> charger backend  -> send - start / stop charging for consumer at device
             charger backend  -> charger device   -> push - change status

  - process billing
  if start - charger backend  -> billing          -> send - initiate charging session
  if stop  - charger backend  -> billing          -> send - charging session complete
             charger backend  -> consumer backend -> send - ack start / stop
             consumer backend -> consumer client  -> push - change status
  if stop  - consumer client  -> billing          -> get  - tally


  Charger device initiates

  - initiate charging
             charger device   -> charger backend  -> post - start / stop charging for consumer at device
             charger backend  -> consumer backend -> send - start / stop charging for consumer at device

  - process billing
  same as when Consumer client initiates
  except that the ack start / stop is missing
 */
