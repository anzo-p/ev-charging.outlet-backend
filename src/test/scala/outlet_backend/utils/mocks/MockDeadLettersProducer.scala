package outlet_backend.utils.mocks

import nl.vroste.zio.kinesis.client.Record
import shared.events.DeadLetterProducer
import zio.{Task, URLayer, ZIO, ZLayer}
import zio.mock.{Mock, Proxy}

object MockDeadLettersProducer extends Mock[DeadLetterProducer] {

  object Send extends Effect[(Record[_], Throwable), Throwable, Unit]

  val compose: URLayer[Proxy, DeadLetterProducer] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new DeadLetterProducer {
        override def send[T](rec: Record[T], th: Throwable): Task[Unit] =
          proxy(Send, rec, th)
      }
    }
}
