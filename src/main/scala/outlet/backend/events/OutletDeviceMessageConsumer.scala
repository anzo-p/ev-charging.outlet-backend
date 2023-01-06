package outlet.backend.events

import outlet.backend.ChargerOutletService
import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import shared.events.ChargingEventProducer
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.OutletDeviceState
import zio.{Task, ZIO, ZLayer}
import zio.aws.sqs.Sqs
import zio.sqs.SqsStream

final case class OutletDeviceMessageConsumer(service: ChargerOutletService, streamWriter: ChargingEventProducer) {

  def consume(msg: OutletDeviceMessage): Task[Unit] =
    msg.outletStatus match {
      case OutletDeviceState.Available =>
        service.setOutletStateUnit(msg.outletId, Some(msg.rfidTag), OutletDeviceState.Available)

      case OutletDeviceState.CablePlugged =>
        // these appear the same case, spare the state, but the dynamodb part is awaiting bugfix on rfid still
        for {
          _ <- ZIO.succeed(println(s"aaaa $msg"))
          _ <- service.setOutletStateUnit(msg.outletId, Some(msg.rfidTag), OutletDeviceState.CablePlugged)
        } yield ()

      //case OutletDeviceState.Cancelled =>
      case OutletDeviceState.ChargingRequested =>
        for {
          initData <- service.setChargingRequested(ChargingEvent.deviceStart(msg.outletId, msg.rfidTag))
          _        <- streamWriter.put(initData.toOutletStatus)
        } yield ()

      case OutletDeviceState.Charging =>
        for {
          report <- service.aggregateConsumption(msg.toChargingEvent)
          _      <- streamWriter.put(report.toOutletStatus)
        } yield ()

      case OutletDeviceState.StoppingRequested =>
        for {
          report <- service.stopCharging(ChargingEvent.deviceStop(msg.outletId, msg.rfidTag))
          _      <- streamWriter.put(report.toOutletStatus)
        } yield ()

      case _ =>
        ZIO.succeed(println(s"OutletDeviceMessageConsumer, unknown event: ${msg.toString}"))
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

object OutletDeviceMessageConsumer {

  val live: ZLayer[ChargerOutletService with ChargingEventProducer, Nothing, OutletDeviceMessageConsumer] =
    ZLayer.fromFunction(OutletDeviceMessageConsumer.apply _)
}
