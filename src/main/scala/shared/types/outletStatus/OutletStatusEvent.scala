package shared.types.outletStatus

import shared.types.enums.{OutletDeviceState, OutletStateRequester}

import java.util.UUID

final case class EventSessionData(
    sessionId: Option[UUID],
    rfidTag: String,
    periodStart: Option[java.time.OffsetDateTime],
    periodEnd: Option[java.time.OffsetDateTime],
    powerConsumption: Double
  )

final case class OutletStatusEvent(
    requester: OutletStateRequester,
    outletId: UUID,
    eventTime: java.time.OffsetDateTime,
    state: OutletDeviceState,
    recentSession: EventSessionData
  )

object OutletStatusEvent {

  def appStart(outletId: UUID, rfidTag: String): OutletStatusEvent =
    OutletStatusEvent(
      requester = OutletStateRequester.Application,
      outletId  = outletId,
      eventTime = java.time.OffsetDateTime.now(),
      state     = OutletDeviceState.ChargingRequested,
      recentSession = EventSessionData(
        sessionId        = Some(UUID.randomUUID()),
        rfidTag          = rfidTag,
        periodStart      = None,
        periodEnd        = None,
        powerConsumption = 0.0
      )
    )

  def appStop(outletId: UUID, rfidTag: String): OutletStatusEvent =
    OutletStatusEvent(
      requester = OutletStateRequester.Application,
      outletId  = outletId,
      eventTime = java.time.OffsetDateTime.now(),
      state     = OutletDeviceState.StoppingRequested,
      recentSession = EventSessionData(
        sessionId        = Some(UUID.randomUUID()),
        rfidTag          = rfidTag,
        periodStart      = None,
        periodEnd        = Some(java.time.OffsetDateTime.now()),
        powerConsumption = 0.0
      )
    )

  def deviceStart(outletId: UUID, rfidTag: String): OutletStatusEvent =
    OutletStatusEvent(
      requester = OutletStateRequester.OutletDevice,
      outletId  = outletId,
      eventTime = java.time.OffsetDateTime.now(),
      state     = OutletDeviceState.ChargingRequested,
      recentSession = EventSessionData(
        sessionId        = None,
        rfidTag          = rfidTag,
        periodStart      = None,
        periodEnd        = None,
        powerConsumption = 0.0
      )
    )

  def deviceStop(outletId: UUID, rfidTag: String): OutletStatusEvent =
    OutletStatusEvent(
      requester = OutletStateRequester.OutletDevice,
      outletId  = outletId,
      eventTime = java.time.OffsetDateTime.now(),
      state     = OutletDeviceState.StoppingRequested,
      recentSession = EventSessionData(
        sessionId        = None,
        rfidTag          = rfidTag,
        periodStart      = None,
        periodEnd        = Some(java.time.OffsetDateTime.now()),
        powerConsumption = 0.0
      )
    )
}
