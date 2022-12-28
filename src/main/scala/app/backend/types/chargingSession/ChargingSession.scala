package app.backend.types.chargingSession

import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState.getPreStatesTo
import shared.types.enums.{OutletDeviceState, PurchaseChannel}
import shared.types.outletStatus.OutletStatusEvent
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class ChargingSession(
    sessionId: UUID,
    customerId: UUID,
    rfidTag: String,
    outletId: UUID,
    state: OutletDeviceState,
    purchaseChannel: PurchaseChannel,
    startTime: Option[java.time.OffsetDateTime],
    endTime: Option[java.time.OffsetDateTime],
    powerConsumption: Option[Double]
  )

object ChargingSession extends DateTimeSchemaImplicits {

  implicit lazy val schema: Schema[ChargingSession] =
    DeriveSchema.gen[ChargingSession]

  def apply(customerId: UUID, outletId: UUID, purchaseChannel: PurchaseChannel): ChargingSession =
    ChargingSession(
      sessionId        = UUID.randomUUID(),
      customerId       = customerId,
      rfidTag          = "",
      outletId         = outletId,
      state            = OutletDeviceState.ChargingRequested,
      purchaseChannel  = purchaseChannel,
      startTime        = None,
      endTime          = None,
      powerConsumption = Some(0.0)
    )

  def fromEvent(customerId: UUID, event: OutletStatusEvent): ChargingSession =
    ChargingSession(
      sessionId        = event.recentSession.sessionId.getOrElse(UUID.randomUUID()),
      customerId       = customerId,
      rfidTag          = event.recentSession.rfidTag,
      outletId         = event.outletId,
      state            = event.state,
      purchaseChannel  = PurchaseChannel.OutletDevice,
      startTime        = event.recentSession.periodStart,
      endTime          = event.recentSession.periodEnd,
      powerConsumption = Some(event.recentSession.powerConsumption)
    )

  def mayTransitionTo(nextState: OutletDeviceState): ChargingSession => Boolean =
    _.state.in(getPreStatesTo(nextState))
}

final case class ChargingSessionUpdate(
    sessionId: UUID,
    rfidTag: String,
    outletId: UUID,
    state: OutletDeviceState,
    startTime: java.time.OffsetDateTime,
    endTime: java.time.OffsetDateTime,
    powerConsumption: Double
  )

object ChargingSessionUpdate extends DateTimeSchemaImplicits {

  //implicit lazy val schema: Schema[ChargingSessionUpdate] =
  //  DeriveSchema.gen[ChargingSessionUpdate]

  def fromEvent(
      event: OutletStatusEvent,
      sessionId: UUID,
      startTime: java.time.OffsetDateTime,
      endTime: java.time.OffsetDateTime
    ): ChargingSessionUpdate =
    ChargingSessionUpdate(
      sessionId        = sessionId,
      rfidTag          = event.recentSession.rfidTag,
      outletId         = event.outletId,
      state            = event.state,
      startTime        = startTime,
      endTime          = endTime,
      powerConsumption = event.recentSession.powerConsumption
    )
}
