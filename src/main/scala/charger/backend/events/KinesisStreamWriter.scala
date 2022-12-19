package charger.backend.events

import nl.vroste.zio.kinesis.client
import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord}
import shared.types.chargingEvent.{ChargingEvent, ProtobufConversions}
import zio._
import zio.aws.cloudwatch.CloudWatch
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis

sealed trait StreamWriter {
  def put(r: ChargingEvent): IO[Throwable, Unit]
}

final case class KinesisStreamWriter(producer: Producer[ChargingEvent]) extends StreamWriter {

  private def put(r: ProducerRecord[ChargingEvent]): IO[Throwable, Unit] =
    producer.produce(r).unit

  override def put(event: ChargingEvent): IO[Throwable, Unit] =
    put(
      ProducerRecord(
        "123",
        event
      )
    )
}

object KinesisStreamWriter {

  val env: ZLayer[Any, Throwable, Kinesis with CloudWatch with DynamoDb with Scope] =
    client.defaultAwsLayer ++ Scope.default

  val make: ZLayer[Scope with Any with Kinesis, Throwable, Producer[ChargingEvent]] = {
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.charger-stream", ProtobufConversions.byteArray)
    }
  }

  val live: ZLayer[Producer[ChargingEvent], Nothing, StreamWriter] =
    ZLayer.fromFunction(KinesisStreamWriter.apply _)
}
