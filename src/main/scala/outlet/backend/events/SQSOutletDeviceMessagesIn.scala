package outlet.backend.events

import outlet.backend.{ChargerOutletService, OutletDeviceMessageConsumer}
import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import shared.events.ChargingEventProducer
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.OutletDeviceState
import zio.aws.sqs.Sqs
import zio.sqs.SqsStream
import zio.{Task, ZIO, ZLayer}

final case class SQSOutletDeviceMessagesIn(toBackend: ChargingEventProducer, outletService: ChargerOutletService)
    extends OutletDeviceMessageConsumer {

  def consume(data: OutletDeviceMessage): Task[Unit] =
    data.outletStatus match {
      case OutletDeviceState.Available =>
        for {
          _ <- outletService.setAvailable(data.outletId)
        } yield ()

      case OutletDeviceState.CablePlugged =>
        for {
          _ <- outletService.setCablePlugged(data.outletId)
        } yield ()

      case OutletDeviceState.DeviceRequestsCharging =>
        for {
          _ <- outletService.checkTransitionOrElse(
                data.outletId,
                OutletDeviceState.DeviceRequestsCharging,
                "Device already has active session")
          _ <- toBackend.put(ChargingEvent.deviceStart(data.outletId, data.rfidTag))
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          report <- outletService.aggregateConsumption(data.toChargingEvent)
          _      <- toBackend.put(report.toOutletStatus)
        } yield ()

      case OutletDeviceState.DeviceRequestsStop =>
        for {
          finalReport <- outletService.stopCharging(ChargingEvent.deviceStop(data.outletId, data.rfidTag))
          _           <- toBackend.put(finalReport.toOutletStatus)
        } yield ()

      case _ =>
        ZIO.succeed(println(s"OutletDeviceMessageConsumer, unknown event: ${data.toString}"))
    }

  def start: ZIO[Sqs, Throwable, Unit] =
    SqsStream("https://sqs.eu-west-1.amazonaws.com/574289728239/ev-charging_device-to-outlet-backend_queue")
      .foreach { message =>
        (for {
          raw <- ZIO.from(message.body.toOption).orElseFail(new Throwable(""))
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
