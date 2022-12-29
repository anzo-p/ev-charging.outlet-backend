package outlet.backend.events

import nl.vroste.zio.kinesis.client.Record
import outlet.backend.ChargerOutletService
import shared.events.OutletEventConsumer
import shared.types.enums.{OutletDeviceState, OutletStateRequester}
import shared.types.outletStatus.OutletStatusEvent
import zio._

final case class DeviceEndOutletEventConsumer(outletService: ChargerOutletService, correspondent: DeviceEndOutletEventProducer)
    extends OutletEventConsumer {

  val applicationName: String = "outlet-backend"

  def follow: OutletStateRequester = OutletStateRequester.Application

  def consume(record: Record[OutletStatusEvent]): Task[Unit] =
    record.data.state match {
      case OutletDeviceState.ChargingRequested =>
        for {
          _ <- ZIO.succeed(println("ChargingRequested")).unit
          _ <- outletService.setCharging(record.data.outletId, record.data.recentSession.rfidTag)
          // else NACK back
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          _ <- ZIO.succeed(println("App has ACKed our Charging Request")).unit
          // http client to call back to aws api gateway websocket to start charging
          _ <- outletService
                .aggregateConsumption(
                  record
                    .data
                    .copy(
                      state         = OutletDeviceState.Charging,
                      recentSession = record.data.recentSession.copy(periodStart = Some(java.time.OffsetDateTime.now()))))
                .unit
        } yield ()

      case OutletDeviceState.StoppingRequested =>
        for {
          _ <- ZIO.succeed(println("StoppingRequested")).unit
          _ <- outletService.stopCharging(record.data)
        } yield ()

      case status =>
        for {
          _ <- ZIO.succeed(println(s"Something else $status")).unit
        } yield ()
    }
}

object DeviceEndOutletEventConsumer {

  val live: ZLayer[ChargerOutletService with DeviceEndOutletEventProducer, Nothing, DeviceEndOutletEventConsumer] =
    ZLayer.fromFunction(DeviceEndOutletEventConsumer.apply _)
}
