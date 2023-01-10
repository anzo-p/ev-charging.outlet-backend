package outlet.backend.events

import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import outlet.backend.{ChargerOutletService, OutletDeviceMessageProducer}
import shared.events.{ChargingEventConsumer, ChargingEventProducer, DeadLetterProducer}
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio._

final case class KinesisChargingEventsIn(
    outletService: ChargerOutletService,
    toDevice: OutletDeviceMessageProducer,
    toBackend: ChargingEventProducer,
    deadLetters: DeadLetterProducer
  ) extends ChargingEventConsumer {

  val applicationName: String = "outlet-backend"

  def follow: EventInitiator = EventInitiator.AppBackend

  def handleTransitionToCharging(event: ChargingEvent): ZIO[Any, Throwable, Unit] =
    for {
      _ <- outletService.setCharging(event.outletId, event.recentSession.rfidTag)
      _ <- toDevice.produce(OutletDeviceMessage.fromChargingEvent(event).copy(outletStatus = OutletDeviceState.Charging))
      // else NACK
    } yield ()

  def consume(data: ChargingEvent): Task[Unit] =
    data.outletState match {
      case OutletDeviceState.AppRequestsCharging =>
        for {
          _ <- outletService.checkTransitionOrElse(
                data.outletId,
                OutletDeviceState.AppRequestsCharging,
                "Device already has active session")
          _ <- handleTransitionToCharging(data)
        } yield ()

      case OutletDeviceState.Charging =>
        handleTransitionToCharging(data)

      case OutletDeviceState.AppRequestsStop =>
        for {
          finalReport <- outletService.stopCharging(data)
          _           <- toDevice.produce(OutletDeviceMessage.fromChargingEvent(data).copy(outletStatus = finalReport.outletState))
          _           <- toBackend.put(finalReport.toOutletStatus)
        } yield ()

      case status =>
        for {
          _ <- ZIO.succeed(println(s"KinesisChargingEventsIn, unknown $status")).unit
        } yield ()
    }
}

object KinesisChargingEventsIn {

  val live
      : ZLayer[ChargingEventProducer with OutletDeviceMessageProducer with ChargerOutletService with DeadLetterProducer, Nothing, KinesisChargingEventsIn] =
    ZLayer.fromFunction(KinesisChargingEventsIn.apply _)
}
