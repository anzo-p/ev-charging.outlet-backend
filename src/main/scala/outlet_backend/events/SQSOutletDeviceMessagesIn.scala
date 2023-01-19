package outlet_backend.events

import outlet_backend.types.outletDeviceMessage.OutletDeviceMessage
import outlet_backend.{ChargerOutletService, OutletDeviceMessageConsumer}
import shared.events.ChargingEventProducer
import shared.types.enums.OutletDeviceState
import zio._
import zio.aws.sqs.Sqs
import zio.sqs.{SqsStream, Utils}

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
          _ <- outletService.checkTransition(event.outletId, OutletDeviceState.DeviceRequestsCharging).flatMap {
                case true  => toBackend.put(event.toChargingEvent)
                case false => ZIO.succeed(()) // this would be OutletDeniesCharging..
              }
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          outlet <- outletService.getOutlet(event.outletId)
          report <- outletService.aggregateConsumption(outlet.getUpdatesFrom(event))
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
    for {
      queueUrl <- Utils.getQueueUrl("ev-charging_device-to-outlet-backend_queue")
      _ <- SqsStream(queueUrl)
            .foreach { message =>
              (for {
                raw <- ZIO.fromOption(message.body.toOption).orElseFail(new Throwable("Problems loading message content"))
                msg <- ZIO.fromEither(OutletDeviceMessage.unapply(raw).left.map(e => new Throwable(s"OutletDeviceMessage error: $e")))
                _   <- consume(msg)
              } yield ())
                .catchAll {
                  case th: Throwable => ZIO.succeed(println(s"OutletDeviceMessageConsumer error: ${th.getMessage}"))
                  case _             => ZIO.succeed(())
                }
            }
    } yield ()
}

object SQSOutletDeviceMessagesIn {

  val live: ZLayer[ChargingEventProducer with ChargerOutletService, Nothing, SQSOutletDeviceMessagesIn] =
    ZLayer.fromFunction(SQSOutletDeviceMessagesIn.apply _)
}
