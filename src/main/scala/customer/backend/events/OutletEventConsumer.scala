package customer.backend.events

import nl.vroste.zio.kinesis.client.zionative.{Consumer, LeaseRepository}
import shared.types.OutletEventSerDes
import zio.Console.printLine
import zio._
import zio.aws.kinesis.Kinesis

object OutletEventConsumer {

  val streamResource = "ev-outlet-app.outlet-events.stream"

  val read: ZIO[Kinesis with LeaseRepository with Any, Throwable, Unit] =
    Consumer
      .shardedStream(
        streamName       = streamResource,
        applicationName  = "my-application",
        deserializer     = OutletEventSerDes.byteArray,
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
}
