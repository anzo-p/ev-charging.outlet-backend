package outlet.backend.mocks

import outlet.backend.OutletDeviceMessageProducer
import outlet.backend.types.outletDeviceMessage.OutletDeviceMessage
import zio.{Task, URLayer, ZIO, ZLayer}
import zio.mock.{Mock, Proxy}

object MockOutletDeviceMessageProducer extends Mock[OutletDeviceMessageProducer] {

  object Produce extends Effect[OutletDeviceMessage, Throwable, Unit]

  val compose: URLayer[Proxy, OutletDeviceMessageProducer] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new OutletDeviceMessageProducer {
        override def produce(message: OutletDeviceMessage): Task[Unit] =
          proxy(Produce, message)
      }
    }
}
