package customer.backend.events

import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord}
import shared.types.{ChargingSession, ChargingSessionSerDes}
import zio._
import zio.aws.kinesis.Kinesis

sealed trait StreamWriter {
  def put(r: ChargingSession): IO[Throwable, Unit]
}

final case class ChargingSessionProducer(producer: Producer[ChargingSession]) extends StreamWriter {

  private def put(r: ProducerRecord[ChargingSession]): IO[Throwable, Unit] =
    producer.produce(r).unit

  override def put(event: ChargingSession): IO[Throwable, Unit] =
    put(
      ProducerRecord(
        "123",
        event
      )
    )
}

object ChargingSessionProducer {

  //val env: ZLayer[Any, Throwable, Kinesis with CloudWatch with DynamoDb with Scope] =
  //  client.defaultAwsLayer ++ Scope.default

  val streamResource = "ev-outlet-app.charging-session.stream"

  val make: ZLayer[Scope with Any with Kinesis, Throwable, Producer[ChargingSession]] = {
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.charging-session.stream", ChargingSessionSerDes.byteArray)
    }
  }

  val live: ZLayer[Producer[ChargingSession], Nothing, StreamWriter] =
    ZLayer.fromFunction(ChargingSessionProducer.apply _)
}
