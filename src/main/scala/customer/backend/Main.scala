package customer.backend

import customer.backend.events.{OutletEventConsumer, OutletStatusProducer}
import customer.backend.http.{ChargingRequestRoutes, CustomerRoutes, CustomerServer}
import customer.backend.services.{DynamoDBChargingService, DynamoDBCustomerService}
import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient
import zio.dynamodb.DynamoDBExecutor

object Main extends ZIOAppDefault {

  val program =
    ZIO.serviceWithZIO[CustomerServer](_.start) *> ZIO.serviceWithZIO[OutletEventConsumer](_.start)

  override def run: URIO[Any, ExitCode] =
    program
      .provide(
        AwsConfig.default,
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
        OutletEventConsumer.live,
        OutletStatusProducer.live,
        OutletStatusProducer.make,
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

/*
  1. naming of models and their dtos, params, events, models, protobufs
  - Customer           OK
  - ChargingSession    - prepare to Delete SerDes and Proto
  - ChargerOutlet      OK
  - UpdateOutletStatus OK

  ok on that removal

  2. flow of models
  - Customer               - dto,    params,       model - to dynamodb
  - REMOVE ChargingSession - dto,            event/model - to dynamodb and to protobuf - can not be downstream derived from upstream OutletStatusEventProto
  - ChargerOutlet          - dto,    params,       model - to dynamodb
  - UpdateOutletStatus     - dto,    params, event/model - to dynamodb and to protobuf - can     be downstream derived from upstream ChargingSessionProto

  ok on that removal

  3. the flows arent thought through
  - device will emit status every minute, app will poll status every minute when open/active
  - should we assume that any part will fetch on every round to complete the dataset to downstream consumers?
  - should we assume that any part will update aggregates to db on every round?

  ok - solve this later

  4. optional fields are just getted mapped - sb ZIOed to Either[Error, A]? and those errors should be handled?

  ok - solve this later

  5. at customer.backend.events.OutletEventConsumer we need to know, when to create the session and when to aggregate to that session

  ok - we will know this bc the producer have checked that this outlet is available and now we attempt to create session and will discover if this user is available

  6. main kinesis flow, maybe we should
  - compose and store ChargingSession only once
    - upon start request from either consumer via app or outlet by token
      - either will send "start" status so it is unambiguous
      - outlet will also have to be available
  - assume that at every other time there is a ChargingSession to aggregate to
  - only send OutletStatusEventProto in kinesis
    - from both ends, to either end, in shared stream
    - outlet.backend consumes if requester is Application
    - customer.backend consumes if OutletDevice
    - bc it contains
      - all the info      the outlet backend should know about  - when talked to
      - all the info that the outlet device         knows about - when talking to backend
 */
