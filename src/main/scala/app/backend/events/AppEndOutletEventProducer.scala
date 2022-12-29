package app.backend.events

import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord}
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio._
import zio.aws.kinesis.Kinesis

sealed trait StreamWriter {
  def put(event: OutletStatusEvent): Task[Unit]
}

final case class AppEndOutletEventProducer(producer: Producer[OutletStatusEvent]) extends StreamWriter {

  private def put(record: ProducerRecord[OutletStatusEvent]): Task[Unit] =
    producer.produce(record).unit

  override def put(event: OutletStatusEvent): Task[Unit] =
    put(
      ProducerRecord(
        "123",
        event
      )
    )
}

object AppEndOutletEventProducer {

  val make: ZLayer[Scope with Any with Kinesis, Throwable, Producer[OutletStatusEvent]] =
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.outlet-events.stream", OutletStatusEventSerDes.byteArray)
    }

  val live: ZLayer[Producer[OutletStatusEvent], Nothing, AppEndOutletEventProducer] =
    ZLayer.fromFunction(AppEndOutletEventProducer.apply _)
}
