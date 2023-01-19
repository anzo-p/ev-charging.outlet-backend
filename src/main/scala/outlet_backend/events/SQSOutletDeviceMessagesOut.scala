package outlet_backend.events

import outlet_backend.OutletDeviceMessageProducer
import outlet_backend.types.outletDeviceMessage.OutletDeviceMessage
import zio._
import zio.aws.sqs.Sqs
import zio.sqs.Utils
import zio.sqs.producer.{Producer, ProducerEvent}

final case class SQSOutletDeviceMessagesOut(producer: Producer[OutletDeviceMessage]) extends OutletDeviceMessageProducer {

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

object SQSOutletDeviceMessagesOut {

  val make: ZLayer[Any with Sqs with Scope, Throwable, Producer[OutletDeviceMessage]] =
    ZLayer.fromZIO {
      Utils.getQueueUrl("ev-charging_outlet-backend-to-device_queue").flatMap { queueUrl =>
        Producer.make(queueUrl, OutletDeviceMessage)
      }
    }

  val live: ZLayer[Producer[OutletDeviceMessage], Nothing, OutletDeviceMessageProducer] =
    ZLayer.fromFunction(SQSOutletDeviceMessagesOut.apply _)
}
