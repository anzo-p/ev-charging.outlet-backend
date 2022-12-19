package charger.backend.events

import nl.vroste.zio.kinesis.client
import nl.vroste.zio.kinesis.client.serde.Serde
import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord}
import zio.Console.printLine
import zio._
import zio.aws.cloudwatch.CloudWatch
import zio.aws.dynamodb.DynamoDb
import zio.aws.kinesis.Kinesis

sealed trait StreamWriter {
  def put(r: String): IO[Throwable, Unit]
}

final case class KinesisStreamWriter(producer: Producer[String]) extends StreamWriter {

  override def put(event: String): IO[Throwable, Unit] = {
    val rec = ProducerRecord(
      "123",
      event
    )

    for {
      _ <- producer.produce(rec)
      _ <- printLine(s"All records in the chunk were produced")
    } yield ()
  }
}

object KinesisStreamWriter {

  val env: ZLayer[Any, Throwable, Kinesis with CloudWatch with DynamoDb with Scope] =
    client.defaultAwsLayer ++ Scope.default

  val make: ZLayer[Scope with Any with Kinesis, Throwable, Producer[String]] = {
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.charger-stream", Serde.asciiString)
    }
  }

  val live: ZLayer[Producer[String], Nothing, StreamWriter] =
    ZLayer.fromFunction(KinesisStreamWriter.apply _)
}
