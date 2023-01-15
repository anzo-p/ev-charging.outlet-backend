package outlet_backend.events

import outlet_backend.types.outletDeviceMessage.OutletDeviceMessage
import outlet_backend.{ChargerOutletService, OutletDeviceMessageProducer}
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

  def handleTransitionToCharging(event: ChargingEvent): Task[Unit] =
    for {
      sessionId <- ZIO.fromOption(event.recentSession.sessionId).orElseFail(new Error("[KinesisChargingEventsIn] no session id"))
      _         <- outletService.setCharging(event.outletId, event.recentSession.rfidTag, sessionId)
      _         <- toDevice.produce(OutletDeviceMessage.fromChargingEvent(event).copy(outletStateChange = OutletDeviceState.Charging))
    } yield ()

  def consume(event: ChargingEvent): Task[Unit] =
    event.outletState match {
      case OutletDeviceState.AppRequestsCharging =>
        for {
          _ <- outletService.checkTransition(event.outletId, OutletDeviceState.AppRequestsCharging).flatMap {
                case true => handleTransitionToCharging(event)
                case false =>
                  toBackend.put(
                    event.copy(
                      initiator   = EventInitiator.OutletBackend,
                      outletState = OutletDeviceState.DeviceDeniesCharging
                    ))
              }
        } yield ()

      case OutletDeviceState.AppDeniesCharging =>
        for {
          _ <- outletService.resetToCablePlugged(event.outletId)
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

  val live
      : ZLayer[ChargerOutletService with OutletDeviceMessageProducer with ChargingEventProducer with DeadLetterProducer, Nothing, KinesisChargingEventsIn] =
    ZLayer.fromFunction(KinesisChargingEventsIn.apply _)
}
