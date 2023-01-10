package outlet.backend.mocks

import outlet.backend.ChargerOutletService
import outlet.backend.types.chargerOutlet.ChargerOutlet
import shared.types.enums.OutletDeviceState
import zio.mock.{Mock, Proxy}
import zio.{Task, URLayer, ZIO, ZLayer}

import java.util.UUID

object MockChargerOutletService extends Mock[ChargerOutletService] {

  object GetOutlet extends Effect[UUID, Throwable, ChargerOutlet]
  object Register extends Effect[ChargerOutlet, Throwable, Unit]
  object CheckTransitionOrElse extends Effect[(UUID, OutletDeviceState, String), Throwable, Unit]
  object SetAvailable extends Effect[UUID, Throwable, Unit]
  object SetCablePlugged extends Effect[UUID, Throwable, Unit]
  object SetCharging extends Effect[(UUID, String), Throwable, Unit]
  object AggregateConsumption extends Effect[ChargerOutlet, Throwable, ChargerOutlet]
  object StopCharging extends Effect[ChargerOutlet, Throwable, ChargerOutlet]

  val compose: URLayer[Proxy, ChargerOutletService] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new ChargerOutletService {
        override def getOutlet(outletId: UUID): Task[ChargerOutlet] =
          proxy(GetOutlet, outletId)

        override def register(outlet: ChargerOutlet): Task[Unit] =
          proxy(Register, outlet)

        override def checkTransitionOrElse(outletId: UUID, nextState: OutletDeviceState, message: String): Task[Unit] =
          proxy(CheckTransitionOrElse, outletId, nextState, message)

        override def setAvailable(outletId: UUID): Task[Unit] =
          proxy(SetAvailable, outletId)

        override def setCablePlugged(outletId: UUID): Task[Unit] =
          proxy(SetCablePlugged, outletId)

        override def setCharging(outletId: UUID, rfidTag: String): Task[Unit] =
          proxy(SetCharging, outletId, rfidTag)

        override def aggregateConsumption(event: ChargerOutlet): Task[ChargerOutlet] =
          proxy(AggregateConsumption, event)

        override def stopCharging(event: ChargerOutlet): Task[ChargerOutlet] =
          proxy(StopCharging, event)
      }
    }
}
