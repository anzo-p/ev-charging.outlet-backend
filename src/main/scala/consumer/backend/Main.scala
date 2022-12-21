package consumer.backend

import consumer.backend.events.{ChargingSessionProducer, OutletEventConsumer}
import consumer.backend.http.ConsumerRoutes
import nl.vroste.zio.kinesis.client.zionative.LeaseRepository
import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient

object Main extends ZIOAppDefault {

  val program: ZIO[Kinesis with LeaseRepository with Any with ConsumerRoutes, Throwable, Unit] =
    ZIO.serviceWithZIO[ConsumerRoutes](_.start) *> OutletEventConsumer.read

  override def run: URIO[Any, ExitCode] =
    program
      .provide(
        AwsConfig.default,
        ChargingSessionProducer.make,
        ChargingSessionProducer.live,
        ConsumerRoutes.live,
        DynamoDb.live,
        DynamoDbLeaseRepository.live,
        Kinesis.live,
        NettyHttpClient.default,
        Scope.default
      )
      .exitCode
}

/*
  sbt run -jvm-debug 9999



  now that we have the protobufs and two kinesis streams
  - should we want to squeeze that into one stream?
    - can we decidee this later?
    - the protobufs and all data classes would become more abstract

  - then consumer store things in dynamodb
  - then consumer emit to kinesis
  - then charger reads from kinesis, stores into dynamodb, and logs that it would push this to device
  - then charger emits to kinesis
  - then consumer reads that
  - then consumer updates things in kinesis
  - then client that polls respective endpoint in consumer can see that the charging event eventually begun


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
