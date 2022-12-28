package app.backend.events

import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord}
import shared.types.outletStatus.{OutletStatusEvent, OutletStatusEventSerDes}
import zio._
import zio.aws.kinesis.Kinesis

sealed trait StreamWriter {
  def put(event: OutletStatusEvent): IO[Throwable, Unit]
}

final case class OutletStatusProducer(producer: Producer[OutletStatusEvent]) extends StreamWriter {

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

object OutletStatusProducer {

  //val env: ZLayer[Any, Throwable, Kinesis with CloudWatch with DynamoDb with Scope] =
  //  client.defaultAwsLayer ++ Scope.default

  val make: ZLayer[Scope with Any with Kinesis, Throwable, Producer[OutletStatusEvent]] =
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.outlet-events.stream", OutletStatusEventSerDes.byteArray)
    }

  val live: ZLayer[Producer[OutletStatusEvent], Nothing, StreamWriter] =
    ZLayer.fromFunction(OutletStatusProducer.apply _)
}
