package outlet.backend.mocks

import shared.events.ChargingEventProducer
import shared.types.chargingEvent.ChargingEvent
import zio.{Task, URLayer, ZIO, ZLayer}
import zio.mock.{Mock, Proxy}

object MockChargingEventProducer extends Mock[ChargingEventProducer] {

  object Put extends Effect[ChargingEvent, Throwable, Unit]

  val compose: URLayer[Proxy, ChargingEventProducer] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new ChargingEventProducer {
        override def put(event: ChargingEvent): Task[Unit] =
          proxy(Put, event)
      }
    }
}
