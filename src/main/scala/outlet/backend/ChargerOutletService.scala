package outlet.backend

import outlet.backend.types.chargerOutlet.ChargerOutlet
import shared.types.enums.OutletDeviceState
import shared.types.chargingEvent.ChargingEvent
import zio.{Task, ZIO}

import java.util.UUID

trait ChargerOutletService {

  def register(outlet: ChargerOutlet): Task[Unit]

  def setOutletStateUnit(outletId: UUID, rfidTag: Option[String], nextState: OutletDeviceState): ZIO[Any, Throwable, Unit]

  def setChargingRequested(event: ChargingEvent): Task[ChargerOutlet]

  def setCharging(outletId: UUID, rfidTag: String): Task[ChargerOutlet]

  def aggregateConsumption(status: ChargingEvent): Task[ChargerOutlet]

  def stopCharging(status: ChargingEvent): Task[ChargerOutlet]
}
