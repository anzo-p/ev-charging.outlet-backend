package app.backend.events

import app.backend.types.chargingSession.ChargingSession
import app.backend.{ChargingService, CustomerService}
import nl.vroste.zio.kinesis.client.Record
import shared.events.{DeadLetterProducer, OutletEventConsumer, OutletEventProducer}
import shared.types.enums.{OutletDeviceState, OutletStateRequester}
import shared.types.outletStatus.OutletStatusEvent
import zio._

final case class AppEndOutletEventConsumer(
    customerService: CustomerService,
    chargingService: ChargingService,
    correspondent: OutletEventProducer,
    deadLetters: DeadLetterProducer
  ) extends OutletEventConsumer {

  val applicationName: String = "app-backend"

  def follow: OutletStateRequester = OutletStateRequester.OutletDevice

  def consume(record: Record[OutletStatusEvent]): Task[Unit] =
    record.data.outletState match {
      case OutletDeviceState.ChargingRequested =>
        for {
          customerId <- customerService.getCustomerIdByRfidTag(record.data.recentSession.rfidTag)
          session    <- ZIO.from(ChargingSession.fromEvent(customerId.get, record.data).copy(sessionState = OutletDeviceState.Charging))
          _          <- chargingService.initialize(session)
          _          <- correspondent.put(session.toEvent)
          // else NACK
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          _ <- chargingService.aggregateSessionTotals(record.data.copy(outletState = OutletDeviceState.Charging))
        } yield ()

      case OutletDeviceState.Finished =>
        for {
          _ <- chargingService.aggregateSessionTotals(record.data.copy(outletState = OutletDeviceState.Finished))
        } yield ()
      case state =>
        for {
          _ <- ZIO.succeed(println(s"Something else $state"))
        } yield ()
    }
}

object AppEndOutletEventConsumer {

  val live
      : ZLayer[CustomerService with ChargingService with OutletEventProducer with DeadLetterProducer, Nothing, AppEndOutletEventConsumer] =
    ZLayer.fromFunction(AppEndOutletEventConsumer.apply _)
}
