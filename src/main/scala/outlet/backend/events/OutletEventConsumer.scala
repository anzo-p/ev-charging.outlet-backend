package outlet.backend.events

import nl.vroste.zio.kinesis.client.Record
import nl.vroste.zio.kinesis.client.zionative.Consumer
import outlet.backend.ChargerOutletService
import shared.types.enums.OutletDeviceState
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio._

final case class OutletEventConsumer(outletService: ChargerOutletService) {

  def consume2(record: Record[OutletStatusEvent]) =
    //ZIO.succeed(s"consuming ... ${println(record)}")
    ZIO.succeed(println(s"consuming ...$record")).unit

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

  val start =
    Consumer
      .shardedStream(
        streamName       = "ev-outlet-app.outlet-events.stream",
        applicationName  = "my-application",
        deserializer     = OutletStatusEventSerDes.byteArray,
        workerIdentifier = "worker1"
      )
      .flatMapPar(Int.MaxValue) {
        case (_, shardStream, checkpointer) =>
          shardStream
          //.tap(record => printLine(s"Processing record $record on shard $shardId"))
            .tap(consume)
            .tap(checkpointer.stage(_))
            .viaFunction(checkpointer.checkpointBatched[Any](nr = 1000, interval = 5.minutes))
      }
      .tap(_ => ZIO.succeed(Thread.sleep(1111))) // slow down for now, later find out why required
      .runDrain
}

object OutletEventConsumer {

  val live =
    ZLayer.fromFunction(OutletEventConsumer.apply _)
}
/*
  missing

  consumer.backend
    - http routes
      - on charging requests
        - dont produce ChargingSession to kinesis
        - produce a will to begin charging
        - other endpoints will do the rest, these will be forwarded to either in backend on frontend

    OK - do more routes later

    - consumer
      - on request to charge from outlet.backend
        - check consumer ok
        - create ChargingSession, store into db
        - produce ok

      - on other events
        - aggregate to consumer data if their status conforms

  outlet.backend
    - http routes
      - we can do this later...

    - consumer
      - on request to charge from consumer.backend
        - check outlet ok
        - tell device to begin charging
          - can log now, later will use zio-http client to send message to aws gateway endpoint
        - produce ok

      - on other statuses from consumer.backend
        - do what they say if device ok
        - log the signals that wold be sent to device
        - i suppose there must be some kinds of acks or else outlet and app might diverge

      - on request to charge from device http
        - check outlet ok
        - produce a will to begin charging

 */
