package outlet.backend.mocks

import outlet.backend.ChargerOutletService
import outlet.backend.types.chargerOutlet.ChargerOutlet
import outlet.backend.types.chargerOutlet.ChargerOutletSpec.testChargerOutlet
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.OutletDeviceState
import zio.{Task, ZIO}

import java.util.UUID

object StubChargerOutletService extends ChargerOutletService {

  override def getOutlet(outletId: UUID): Task[Option[ChargerOutlet]] =
    ZIO.succeed(Some(testChargerOutlet))

  override def register(outlet: ChargerOutlet): Task[Unit] =
    ZIO.succeed(())

  override def checkTransitionOrElse(outletId: UUID, nextState: OutletDeviceState, message: String): Task[Unit] =
    ZIO.succeed(())

  override def setAvailable(outletId: UUID): Task[Unit] =
    ZIO.succeed(())

  override def setCablePlugged(outletId: UUID): Task[Unit] =
    ZIO.succeed(())

  override def setCharging(outletId: UUID, rfidTag: String): Task[Unit] =
    ZIO.succeed(())

  override def aggregateConsumption(status: ChargingEvent): Task[ChargerOutlet] =
    ZIO.succeed(testChargerOutlet)

  override def stopCharging(status: ChargingEvent): Task[ChargerOutlet] =
    ZIO.succeed(testChargerOutlet)
}
