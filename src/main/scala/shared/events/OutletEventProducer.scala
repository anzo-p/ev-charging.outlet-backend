package shared.events

import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord}
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio._
import zio.aws.kinesis.Kinesis

final case class OutletEventProducer(producer: Producer[OutletStatusEvent]) {

  def put(event: OutletStatusEvent): Task[Unit] =
    producer
      .produce(ProducerRecord("123", event))
      .unit
}

object OutletEventProducer {

  val make: ZLayer[Scope with Any with Kinesis, Throwable, Producer[OutletStatusEvent]] =
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.outlet-events.stream", OutletStatusEventSerDes.byteArray)
    }

  val live: ZLayer[Producer[OutletStatusEvent], Nothing, OutletEventProducer] =
    ZLayer.fromFunction(OutletEventProducer.apply _)
}
