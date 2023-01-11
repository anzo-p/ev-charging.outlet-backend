package outlet_backend

import outlet_backend.types.chargerOutlet.ChargerOutlet
import shared.types.enums.OutletDeviceState
import zio.Task

import java.util.UUID

trait ChargerOutletService {

  def getOutlet(outletId: UUID): Task[ChargerOutlet]

  def register(outlet: ChargerOutlet): Task[Unit]

  def checkTransitionOrElse(outletId: UUID, nextState: OutletDeviceState, message: String): Task[Unit]

  def setAvailable(outletId: UUID): Task[Unit]

  def setCablePlugged(outletId: UUID): Task[Unit]

  def setCharging(outletId: UUID, rfidTag: String): Task[Unit]

  def aggregateConsumption(outlet: ChargerOutlet): Task[ChargerOutlet]

  def stopCharging(outlet: ChargerOutlet): Task[ChargerOutlet]
}
