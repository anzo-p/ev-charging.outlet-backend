package outlet.backend

import outlet.backend.types.ChargerOutlet
import shared.types.enums.OutletDeviceState
import shared.types.outletStatus.OutletStatusEvent
import zio.{Task, ZIO}

import java.util.UUID

trait ChargerOutletService {

  def register(outlet: ChargerOutlet): Task[ChargerOutlet]

  def setOutletStateUnit(outletId: UUID, rfidTag: Option[String], nextState: OutletDeviceState): ZIO[Any, Throwable, Unit]

  def setChargingRequested(event: OutletStatusEvent): Task[ChargerOutlet]

  def setCharging(outletId: UUID, rfidTag: String): Task[ChargerOutlet]

  def aggregateConsumption(status: OutletStatusEvent): Task[ChargerOutlet]

  def stopCharging(status: OutletStatusEvent): Task[ChargerOutlet]
}
