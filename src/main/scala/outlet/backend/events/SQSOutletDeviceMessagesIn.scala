package outlet.backend.events

import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import outlet.backend.{ChargerOutletService, OutletDeviceMessageConsumer}
import shared.events.ChargingEventProducer
import shared.types.enums.OutletDeviceState
import zio.aws.sqs.Sqs
import zio.sqs.SqsStream
import zio.{Task, ZIO, ZLayer}

final case class SQSOutletDeviceMessagesIn(outletService: ChargerOutletService, toBackend: ChargingEventProducer)
    extends OutletDeviceMessageConsumer {

  def consume(event: OutletDeviceMessage): Task[Unit] =
    event.outletStateChange match {
      case OutletDeviceState.Available =>
        for {
          _ <- outletService.setAvailable(event.outletId)
        } yield ()

      case OutletDeviceState.CablePlugged =>
        for {
          _ <- outletService.setCablePlugged(event.outletId)
        } yield ()

      case OutletDeviceState.DeviceRequestsCharging =>
        for {
          _ <- outletService.checkTransitionOrElse(
                event.outletId,
                OutletDeviceState.DeviceRequestsCharging,
                "Device already has active session")
          _ <- toBackend.put(event.toChargingEvent)
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          outlet <- outletService.getOutlet(event.outletId)
          report <- outletService.aggregateConsumption(outlet.setChargingFrom(event))
          _      <- toBackend.put(report.toChargingEvent)
        } yield ()

      case OutletDeviceState.DeviceRequestsStop =>
        for {
          outlet      <- outletService.getOutlet(event.outletId)
          finalReport <- outletService.stopCharging(outlet.getUpdatesFrom(event))
          _           <- toBackend.put(finalReport.toChargingEvent)
        } yield ()

      case _ =>
        ZIO.succeed(println(s"OutletDeviceMessageConsumer, unknown event: ${event.toString}"))
    }

  def start: ZIO[Sqs, Throwable, Unit] =
    SqsStream("https://sqs.eu-west-1.amazonaws.com/574289728239/ev-charging_device-to-outlet-backend_queue")
      .foreach { message =>
        (for {
          raw <- ZIO.fromOption(message.body.toOption).orElseFail(new Throwable("Problems loading message content"))
          msg <- ZIO.fromEither(OutletDeviceMessage.unapply(raw).left.map(e => new Throwable(s"OutletDeviceMessage unapply error: $e")))
          _   <- consume(msg)
        } yield ())
          .catchAll {
            case th: Throwable => ZIO.succeed(println(s"OutletDeviceMessageConsumer error: ${th.getMessage}"))
            case _             => ZIO.succeed(())
          }
      }
}

object SQSOutletDeviceMessagesIn {

  val live: ZLayer[ChargingEventProducer with ChargerOutletService, Nothing, SQSOutletDeviceMessagesIn] =
    ZLayer.fromFunction(SQSOutletDeviceMessagesIn.apply _)
}
