package shared.types.outletStatus

import shared.types.enums.{OutletDeviceState, OutletStateRequester}

import java.util.UUID

final case class EventSessionData(
    sessionId: Option[UUID],
    rfidTag: String,
    periodStart: java.time.OffsetDateTime,
    periodEnd: Option[java.time.OffsetDateTime],
    powerConsumption: Double
  )

final case class OutletStatusEvent(
    requester: OutletStateRequester,
    outletId: UUID,
    state: OutletDeviceState,
    recentSession: EventSessionData
  )

object OutletStatusEvent {

  def deviceStart(outletId: UUID, rfidTag: String): OutletStatusEvent =
    OutletStatusEvent(
      requester = OutletStateRequester.OutletDevice,
      outletId  = outletId,
      state     = OutletDeviceState.ChargingRequested,
      recentSession = EventSessionData(
        sessionId        = None,
        rfidTag          = rfidTag,
        periodStart      = java.time.OffsetDateTime.now(),
        periodEnd        = None,
        powerConsumption = 0.0
      )
    )

  def deviceStop(outletId: UUID, rfidTag: String): OutletStatusEvent =
    OutletStatusEvent(
      requester = OutletStateRequester.OutletDevice,
      outletId  = outletId,
      state     = OutletDeviceState.Finished,
      recentSession = EventSessionData(
        sessionId        = None,
        rfidTag          = rfidTag,
        periodStart      = java.time.OffsetDateTime.now(),
        periodEnd        = Some(java.time.OffsetDateTime.now()),
        powerConsumption = 0.0
      )
    )
}
