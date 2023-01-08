package outlet.backend

import outlet.backend.types.chargerOutlet.ChargerOutlet
import shared.types.chargingEvent.ChargingEvent
import shared.types.enums.OutletDeviceState
import zio.Task

import java.util.UUID

trait ChargerOutletService {

  def register(outlet: ChargerOutlet): Task[Unit]

  def checkTransitionOrElse(outletId: UUID, nextState: OutletDeviceState, message: String): Task[Unit]

  def setAvailable(outletId: UUID): Task[Unit]

  def setCablePlugged(outletId: UUID): Task[Unit]

  def setCharging(outletId: UUID, rfidTag: String): Task[Unit]

  def aggregateConsumption(status: ChargingEvent): Task[ChargerOutlet]

  def stopCharging(status: ChargingEvent): Task[ChargerOutlet]
}
