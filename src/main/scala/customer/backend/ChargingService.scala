package customer.backend

import shared.types.ChargingSession
import zio.IO

import java.util.UUID

trait ChargingService {
  def hasActiveSession(customerId: UUID): IO[Throwable, Boolean]

  def add(sessionData: ChargingSession): IO[Throwable, ChargingSession]

  def update(sessionId: UUID, session: ChargingSession): IO[Throwable, ChargingSession]
}
