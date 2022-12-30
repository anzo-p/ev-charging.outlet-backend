package shared.events

import nl.vroste.zio.kinesis.client.serde.Serde
import nl.vroste.zio.kinesis.client.{Producer, ProducerRecord, Record}
import shared.types.deadLetters.DeadLetterMessage
import zio.aws.kinesis.Kinesis
import zio.{Scope, Task, ZLayer}

final case class DeadLetterProducer(producer: Producer[String]) {
  import zio.json._

  private def put(message: String): Task[Unit] =
    producer
      .produce(ProducerRecord("123", message))
      .unit

  def send[T](rec: Record[T], th: Throwable): Task[Unit] =
    put(DeadLetterMessage.make[T](rec, th).toJson)
}

object DeadLetterProducer {

  val make: ZLayer[Scope with Any with Kinesis, Throwable, Producer[String]] =
    ZLayer.fromZIO {
      Producer.make("ev-outlet-app.dead-letters.stream", Serde.asciiString)
    }

  val live: ZLayer[Producer[String], Nothing, DeadLetterProducer] =
    ZLayer.fromFunction(DeadLetterProducer.apply _)
}
