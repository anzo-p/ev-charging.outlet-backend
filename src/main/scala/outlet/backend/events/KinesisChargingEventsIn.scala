package outlet.backend.events

import outlet.backend.{ChargerOutletService, OutletDeviceMessageProducer}
import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import shared.events.{ChargingEventConsumer, ChargingEventProducer, DeadLetterProducer}
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio._

final case class KinesisChargingEventsIn(
    outletService: ChargerOutletService,
    correspondent: ChargingEventProducer,
    deadLetters: DeadLetterProducer,
    deviceWhisperer: OutletDeviceMessageProducer
  ) extends ChargingEventConsumer {

  val applicationName: String = "outlet-backend"

  def follow: EventInitiator = EventInitiator.Application

  def consume(data: ChargingEvent): Task[Unit] =
    data.outletState match {
      case OutletDeviceState.ChargingRequested =>
        for {
          _ <- ZIO.succeed(println("ChargingRequested")).unit
          // App has requested
          // check and set in dynamodb
          // tell device to start charging
          // ack or nack back to app
          // be explicit about target status right here
          _ <- outletService.setCharging(data.outletId, data.recentSession.rfidTag)
          _ <- deviceWhisperer.produce(OutletDeviceMessage.fromChargingEvent(data)) //.outletId, OutletDeviceState.Charging))
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          _ <- ZIO.succeed(println("App has ACKed our Charging Request")).unit
          // we have requested, app has approved
          // tell device to start charging
          // set charging in dynamodb
          _ <- outletService.setCharging(data.outletId, data.recentSession.rfidTag)
          _ <- deviceWhisperer.produce(OutletDeviceMessage.fromChargingEvent(data)) //.outletId, OutletDeviceState.Charging))
        } yield ()

      case OutletDeviceState.StoppingRequested =>
        for {
          _ <- ZIO.succeed(println("StoppingRequested")).unit
          // app has requested a stop
          // tell device to stop charging
          // set to stop in dynamodb
          finalReport <- outletService.stopCharging(data)
          _           <- correspondent.put(finalReport.toOutletStatus.copy(outletState = OutletDeviceState.Finished))
        } yield ()

      case status =>
        for {
          _ <- ZIO.succeed(println(s"Something else $status")).unit
        } yield ()
    }
}

object KinesisChargingEventsIn {

  val live
      : ZLayer[ChargerOutletService with ChargingEventProducer with DeadLetterProducer with OutletDeviceMessageProducer, Nothing, KinesisChargingEventsIn] =
    ZLayer.fromFunction(KinesisChargingEventsIn.apply _)
}
