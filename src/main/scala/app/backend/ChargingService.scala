package app.backend

import app.backend.types.chargingSession.ChargingSession
import shared.types.enums.OutletDeviceState
import shared.types.outletStatus.OutletStatusEvent
import zio.Task

import java.util.UUID

trait ChargingService {
  def getHistory(customerId: UUID): Task[List[ChargingSession]]

  def initialize(session: ChargingSession): Task[Unit]

  def getSession(sessionId: UUID): Task[ChargingSession]

  def aggregateSessionTotals(status: OutletStatusEvent, targetState: OutletDeviceState): Task[Unit]

  def setStopRequested(sessionId: UUID): Task[Unit]
}
