package shared.events

import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord}
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio.aws.kinesis.Kinesis
import zio.{Scope, Task, ZLayer}

final case class OutletEventProducer(producer: Producer[OutletStatusEvent]) {

  private def put(record: ProducerRecord[OutletStatusEvent]): Task[Unit] =
    producer.produce(record).unit

  def put(event: OutletStatusEvent): Task[Unit] =
    put(
      ProducerRecord(
        "123",
        event
      )
    )
}

object OutletEventProducer {

  val make: ZLayer[Scope with Any with Kinesis, Throwable, Producer[OutletStatusEvent]] =
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.outlet-events.stream", OutletStatusEventSerDes.byteArray)
    }

  val live: ZLayer[Producer[OutletStatusEvent], Nothing, OutletEventProducer] =
    ZLayer.fromFunction(OutletEventProducer.apply _)
}
