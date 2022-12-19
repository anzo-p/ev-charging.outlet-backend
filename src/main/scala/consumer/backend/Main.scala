package consumer.backend

import nl.vroste.zio.kinesis.client.serde.Serde
import nl.vroste.zio.kinesis.client.zionative.Consumer
import nl.vroste.zio.kinesis.client.zionative.leaserepository.DynamoDbLeaseRepository
import zio.Console.printLine
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis
import zio.aws.netty.NettyHttpClient

object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    Consumer
      .shardedStream(
        streamName       = "ev-outlet-app.charger-stream",
        applicationName  = "my-application",
        deserializer     = Serde.asciiString,
        workerIdentifier = "worker1"
      )
      .flatMapPar(Int.MaxValue) {
        case (shardId, shardStream, checkpointer) =>
          shardStream
            .tap(record => printLine(s"Processing record $record on shard $shardId"))
            .tap(checkpointer.stage(_))
            .viaFunction(checkpointer.checkpointBatched[Any](nr = 1000, interval = 5.minutes))
      }
      .tap(_ => ZIO.succeed(Thread.sleep(1111))) // slow down for now, later find out why required
      .runDrain
      .provide(
        NettyHttpClient.default,
        AwsConfig.default,
        DynamoDbLeaseRepository.live,
        DynamoDb.live,
        Kinesis.live
      )
      .exitCode
}

/*
sbt run -jvm-debug 9999


  serve rest api
  - post - consumer client requests begins charging { consumer data }
    - send to kinesis: consumer requests start charging from device id

  - post - consumer client requests stops charging { session id or consumer data }
    - send to kinesis: consumer requests stop charging at device id

  - get consumer clients charging history

  read kinesis
  - charger backend has issued to start charging
    - push to device

  - charger backend has issued a stop charging
    - push to device- push to device

  persist in dynamodb
  - consumer
    - active events
    - history, paginated
    - tally of charging and expenses
 */
/*
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
  1 charger with routes - without model, dto, validation
  2 plus writer without protobuf
  3 plus consumer main, reader
  4 plus protobuf, models, dto
  then add validation but dont commit yet
 */
