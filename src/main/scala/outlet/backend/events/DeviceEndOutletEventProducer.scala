package outlet.backend.events

import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord}
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio._

sealed trait StreamWriter {
  def put(event: OutletStatusEvent): IO[Throwable, Unit]
}

final case class DeviceEndOutletEventProducer(producer: Producer[OutletStatusEvent]) extends StreamWriter {

  private def put(record: ProducerRecord[OutletStatusEvent]): IO[Throwable, Unit] =
    producer.produce(record).unit

  override def put(event: OutletStatusEvent): IO[Throwable, Unit] =
    put(
      ProducerRecord(
        "123",
        event
      )
    )
}

object DeviceEndOutletEventProducer {

  val make =
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.outlet-events.stream", OutletStatusEventSerDes.byteArray)
    }

  val live: ZLayer[Producer[OutletStatusEvent], Nothing, DeviceEndOutletEventProducer] =
    ZLayer.fromFunction(DeviceEndOutletEventProducer.apply _)
}
