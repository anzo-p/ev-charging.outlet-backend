package customer.backend

import customer.backend.types.chargingSession.ChargingSession
import zio.Task

import java.util.UUID

trait ChargingService {

  def initialize(session: ChargingSession): Task[Unit]

  def setStopRequested(sessionId: UUID): Task[Unit]

  def getHistory(customerId: UUID): Task[List[ChargingSession]]
}
