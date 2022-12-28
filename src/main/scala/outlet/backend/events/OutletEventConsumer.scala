package outlet.backend.events

import nl.vroste.zio.kinesis.client.Record
import nl.vroste.zio.kinesis.client.zionative.{Consumer, LeaseRepository}
import outlet.backend.ChargerOutletService
import shared.types.enums.{OutletDeviceState, OutletStateRequester}
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio.Console.printLine
import zio._
import zio.aws.kinesis.Kinesis

final case class OutletEventConsumer(outletService: ChargerOutletService) {

  def consume(record: Record[OutletStatusEvent]): Task[Unit] =
    record.data.state match {
      case OutletDeviceState.ChargingRequested =>
        for {
          _ <- ZIO.succeed(println("ChargingRequested")).unit
          _ <- outletService.setChargingRequested(record.data.outletId, record.data.recentSession.rfidTag) // what with errors?
          // already <- chargingService.hasActiveSession(dto.customerId) //.mapError(serverError)
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          _ <- ZIO.succeed(println("Charging")).unit
        } yield ()

      case OutletDeviceState.StoppingRequested =>
        for {
          _ <- ZIO.succeed(println("StoppingRequested")).unit
        } yield ()

      case status =>
        for {
          _ <- ZIO.succeed(println(s"Something else $status")).unit
        } yield ()
    }

  val start: URIO[Kinesis with LeaseRepository with Any, ExitCode] =
    Consumer
      .shardedStream(
        streamName       = "ev-outlet-app.outlet-events.stream",
        applicationName  = "outlet-backend",
        deserializer     = OutletStatusEventSerDes.byteArray,
        workerIdentifier = "worker1"
      )
      .flatMapPar(4) {
        case (shardId, shardStream, checkpointer) =>
          shardStream
            .filter(_.data.requester == OutletStateRequester.Application)
            .tap(record => printLine(s"Processing record $record on shard $shardId"))
            .tap(_ => ZIO.succeed(Thread.sleep(1111))) // slow down for now, later find out why required
            .tap(consume)
            .tap(checkpointer.stage(_))
            .viaFunction(checkpointer.checkpointBatched[Any](nr = 1000, interval = 5.minutes))
      }
      .tap(_ => ZIO.succeed(Thread.sleep(1111))) // slow down for now, later find out why required
      .runDrain
      .exitCode
}

object OutletEventConsumer {

  val live: ZLayer[ChargerOutletService, Nothing, OutletEventConsumer] =
    ZLayer.fromFunction(OutletEventConsumer.apply _)
}
