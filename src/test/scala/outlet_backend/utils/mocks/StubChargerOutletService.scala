package outlet_backend.utils.mocks

import outlet_backend.ChargerOutletService
import outlet_backend.types.chargerOutlet.Fixtures.fixtureBasicChargerOutlet
import outlet_backend.types.chargerOutlet.ChargerOutlet
import shared.types.enums.OutletDeviceState
import zio.{Task, ZIO}

import java.util.UUID

object StubChargerOutletService extends ChargerOutletService {

  override def getOutlet(outletId: UUID): Task[ChargerOutlet] =
    ZIO.succeed(fixtureBasicChargerOutlet)

  override def register(outlet: ChargerOutlet): Task[Unit] =
    ZIO.succeed(())

  override def checkTransition(outletId: UUID, nextState: OutletDeviceState): Task[Boolean] =
    ZIO.succeed(true)

  override def setAvailable(outletId: UUID): Task[Unit] =
    ZIO.succeed(())

  override def setCablePlugged(outletId: UUID): Task[Unit] =
    ZIO.succeed(())

  override def resetToCablePlugged(outletId: UUID): Task[Unit] =
    ZIO.succeed(())

  override def setCharging(outletId: UUID, rfidTag: String, sessionId: UUID): Task[Unit] =
    ZIO.succeed(())

  override def aggregateConsumption(outlet: ChargerOutlet): Task[ChargerOutlet] =
    ZIO.succeed(fixtureBasicChargerOutlet)

  override def stopCharging(outlet: ChargerOutlet): Task[ChargerOutlet] =
    ZIO.succeed(fixtureBasicChargerOutlet)
}
