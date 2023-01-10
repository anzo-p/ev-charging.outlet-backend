package outlet.backend.events

import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import outlet.backend.{ChargerOutletService, OutletDeviceMessageProducer}
import shared.events.{ChargingEventConsumer, DeadLetterProducer}
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio._

final case class KinesisChargingEventsIn(
    outletService: ChargerOutletService,
    toDevice: OutletDeviceMessageProducer,
    deadLetters: DeadLetterProducer
  ) extends ChargingEventConsumer {

  val applicationName: String = "outlet-backend"

  def follow: EventInitiator = EventInitiator.AppBackend

  def handleTransitionToCharging(event: ChargingEvent): Task[Unit] =
    for {
      _ <- outletService.setCharging(event.outletId, event.recentSession.rfidTag)
      _ <- toDevice.produce(OutletDeviceMessage.fromChargingEvent(event).copy(outletStateChange = OutletDeviceState.Charging))
      // else NACK
    } yield ()

  def consume(event: ChargingEvent): Task[Unit] =
    event.outletState match {
      case OutletDeviceState.AppRequestsCharging =>
        for {
          _ <- outletService.checkTransitionOrElse(
                event.outletId,
                OutletDeviceState.AppRequestsCharging,
                "Device already has active session")
          _ <- handleTransitionToCharging(event)
        } yield ()

      case OutletDeviceState.Charging =>
        handleTransitionToCharging(event)

      case OutletDeviceState.AppRequestsStop =>
        for {
          _ <- toDevice.produce(OutletDeviceMessage.fromChargingEvent(event).copy(outletStateChange = OutletDeviceState.AppRequestsStop))
        } yield ()

      case status =>
        for {
          _ <- ZIO.succeed(println(s"KinesisChargingEventsIn, unknown $status"))
        } yield ()
    }
}

object KinesisChargingEventsIn {

  val live: ZLayer[ChargerOutletService with OutletDeviceMessageProducer with DeadLetterProducer, Nothing, KinesisChargingEventsIn] =
    ZLayer.fromFunction(KinesisChargingEventsIn.apply _)
}
