package outlet.backend.events

import nl.vroste.zio.kinesis.client.Record
import outlet.backend.ChargerOutletService
import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import shared.events.{ChargingEventConsumer, ChargingEventProducer, DeadLetterProducer}
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio._

final case class DeviceEndChargingEventConsumer(
    outletService: ChargerOutletService,
    correspondent: ChargingEventProducer,
    deadLetters: DeadLetterProducer,
    deviceWhisperer: OutletDeviceMessageProducer
  ) extends ChargingEventConsumer {

  val applicationName: String = "outlet-backend"

  def follow: EventInitiator = EventInitiator.Application

  def consume(record: Record[ChargingEvent]): Task[Unit] =
    record.data.outletState match {
      case OutletDeviceState.ChargingRequested =>
        for {
          _ <- ZIO.succeed(println("ChargingRequested")).unit
          _ <- deviceWhisperer.produce(OutletDeviceMessage.fromChargingEvent(record.data)) //.outletId, OutletDeviceState.Charging))
          _ <- outletService.setCharging(record.data.outletId, record.data.recentSession.rfidTag)
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          _ <- ZIO.succeed(println("App has ACKed our Charging Request")).unit
          _ <- deviceWhisperer.produce(OutletDeviceMessage.fromChargingEvent(record.data)) //.outletId, OutletDeviceState.Charging))
          _ <- outletService.setCharging(record.data.outletId, record.data.recentSession.rfidTag)
        } yield ()

      case OutletDeviceState.StoppingRequested =>
        for {
          _           <- ZIO.succeed(println("StoppingRequested")).unit
          finalReport <- outletService.stopCharging(record.data)
          _           <- correspondent.put(finalReport.toOutletStatus.copy(outletState = OutletDeviceState.Finished))
        } yield ()

      case status =>
        for {
          _ <- ZIO.succeed(println(s"Something else $status")).unit
        } yield ()
    }
}

object DeviceEndChargingEventConsumer {

  val live
      : ZLayer[ChargerOutletService with ChargingEventProducer with DeadLetterProducer with OutletDeviceMessageProducer, Nothing, DeviceEndChargingEventConsumer] =
    ZLayer.fromFunction(DeviceEndChargingEventConsumer.apply _)
}
