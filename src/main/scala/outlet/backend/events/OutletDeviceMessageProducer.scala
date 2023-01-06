package outlet.backend.events

import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import zio._
import zio.aws.sqs.Sqs
import zio.sqs.producer.{Producer, ProducerEvent}

final case class OutletDeviceMessageProducer(producer: Producer[OutletDeviceMessage]) {

  def produce(message: OutletDeviceMessage): Task[Unit] =
    producer
      .produce(
        ProducerEvent[OutletDeviceMessage](
          message,
          attributes      = Map.empty,
          groupId         = None,
          deduplicationId = None
        ))
      .unit
}

object OutletDeviceMessageProducer {

  val make: ZLayer[Any with Sqs with Scope, Throwable, Producer[OutletDeviceMessage]] =
    ZLayer.fromZIO {
      Producer.make(
        "https://sqs.eu-west-1.amazonaws.com/574289728239/ev-charging_outlet-backend-to-device_queue",
        OutletDeviceMessage
      )
    }

  val live: ZLayer[Producer[OutletDeviceMessage], Nothing, OutletDeviceMessageProducer] =
    ZLayer.fromFunction(OutletDeviceMessageProducer.apply _)
}
