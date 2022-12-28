package outlet.backend

import outlet.backend.types.ChargerOutlet
import shared.types.outletStatus.OutletStatusEvent
import zio.Task

import java.util.UUID

trait ChargerOutletService {

  def register(outlet: ChargerOutlet): Task[ChargerOutlet]

  def setAvailable(outletId: UUID): Task[Unit]

  def setCablePlugged(outletId: UUID): Task[Unit]

  def setChargingRequested(outletId: UUID, rfidToken: String): Task[ChargerOutlet]

  def beginCharging(outletId: UUID): Task[Unit]

  def aggregateConsumption(status: OutletStatusEvent): Task[ChargerOutlet]

  def stopCharging(status: OutletStatusEvent): Task[ChargerOutlet]
}
