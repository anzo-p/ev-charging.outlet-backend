package app.backend.events

import app.backend.types.chargingSession.ChargingSession
import app.backend.{ChargingService, CustomerService}
import nl.vroste.zio.kinesis.client.Record
import shared.events.OutletEventConsumer
import shared.types.enums.{OutletDeviceState, OutletStateRequester}
import shared.types.outletStatus.OutletStatusEvent
import zio._

final case class AppEndOutletEventConsumer(
    customerService: CustomerService,
    chargingService: ChargingService,
    correspondent: AppEndOutletEventProducer
  ) extends OutletEventConsumer {

  val applicationName: String = "app-backend"

  def follow: OutletStateRequester = OutletStateRequester.OutletDevice

  def consume(record: Record[OutletStatusEvent]): Task[Unit] =
    record.data.state match {
      case OutletDeviceState.ChargingRequested =>
        for {
          _          <- ZIO.succeed(println("ChargingRequested")).unit
          _          <- ZIO.succeed(println(record.data)).unit
          customerId <- customerService.getCustomerIdByRfidTag(record.data.recentSession.rfidTag)
          // the fundamental question is, what should happen if we encounter a throwable?
          session <- ZIO.from(ChargingSession.fromEvent(customerId.get /*FIXME*/, record.data).copy(state = OutletDeviceState.Charging))
          _       <- chargingService.initialize(session)
          _       <- correspondent.put(session.toEvent)
          // else NACK
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          _ <- ZIO.succeed(println("Charging")).unit
          _ <- chargingService.aggregateSessionTotals(record.data.copy(state = OutletDeviceState.Charging))
        } yield ()

      case OutletDeviceState.Finished =>
        for {
          _ <- ZIO.succeed(println("StoppingRequested")).unit
          _ <- chargingService.aggregateSessionTotals(record.data.copy(state = OutletDeviceState.Finished))
        } yield ()

      case state =>
        for {
          _ <- ZIO.succeed(println(s"Something else $state")).unit
        } yield ()
    }
}

object AppEndOutletEventConsumer {

  val live: ZLayer[CustomerService with ChargingService with AppEndOutletEventProducer, Nothing, AppEndOutletEventConsumer] =
    ZLayer.fromFunction(AppEndOutletEventConsumer.apply _)
}
