package customer.backend.events

import customer.backend.{ChargingService, CustomerService}
import nl.vroste.zio.kinesis.client.Record
import nl.vroste.zio.kinesis.client.zionative.Consumer
import shared.types.enums.OutletDeviceState
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio.Console.printLine
import zio._

final case class OutletEventConsumer(customerService: CustomerService, chargingService: ChargingService) {

  def consume(record: Record[OutletStatusEvent]): Task[Unit] = {
    ZIO.succeed {
      record.data.state match {
        case OutletDeviceState.ChargingRequested =>
          for {
            _ <- ZIO.succeed(println("ChargingRequested"))
            //_ <- customerService.getById(UUID.randomUUID()) //.orElseFail(invalidPayload("this customer doesn't exist"))
            // already <- chargingService.hasActiveSession(dto.customerId) //.mapError(serverError)
          } yield ()

        case OutletDeviceState.Charging =>
          for {
            _ <- ZIO.succeed(println("Charging"))
          } yield ()

        case OutletDeviceState.StoppingRequested =>
          for {
            _ <- ZIO.succeed(println("StoppingRequested"))
          } yield ()

        case state =>
          for {
            _ <- ZIO.succeed(println(s"Something else $state"))
          } yield ()
      }
    }.unit
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
        case (shardId, shardStream, checkpointer) =>
          shardStream
            .tap(record => printLine(s"Processing record $record on shard $shardId"))
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
