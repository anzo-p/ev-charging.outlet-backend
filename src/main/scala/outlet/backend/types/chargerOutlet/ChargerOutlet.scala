package outlet.backend.types.chargerOutlet

import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.chargingEvent.{ChargingEvent, EventSession}
import shared.types.enums.{EventInitiator, OutletDeviceState}
import shared.types.outletStateMachine.OutletStateMachine
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class ChargerOutlet(
    outletId: UUID,
    chargerGroupId: UUID,
    outletCode: String,
    address: String,
    maxPower: Double,
    outletState: OutletDeviceState,
    sessionId: Option[UUID],
    rfidTag: String,
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime],
    powerConsumption: Double,
    totalChargingEvents: Long,
    totalPowerConsumption: Double
  ) extends OutletStateMachine {

  def toOutletStatus: ChargingEvent =
    ChargingEvent(
      initiator   = EventInitiator.OutletDevice,
      outletId    = this.outletId,
      outletState = this.outletState,
      recentSession = EventSession(
        sessionId        = this.sessionId,
        rfidTag          = this.rfidTag,
        periodStart      = this.startTime,
        periodEnd        = this.endTime,
        powerConsumption = this.powerConsumption
      )
    )
}

object ChargerOutlet extends DateTimeSchemaImplicits {

  implicit lazy val schema: Schema[ChargerOutlet] = DeriveSchema.gen[ChargerOutlet]

  def apply(
      chargerGroupId: UUID,
      outletCode: String,
      address: String,
      maxPower: Double
    ): ChargerOutlet =
    ChargerOutlet(
      outletId              = UUID.randomUUID(),
      chargerGroupId        = chargerGroupId,
      outletCode            = outletCode,
      address               = address,
      maxPower              = maxPower,
      outletState           = OutletDeviceState.Available,
      sessionId             = None,
      rfidTag               = "",
      startTime             = java.time.OffsetDateTime.now(),
      endTime               = None,
      powerConsumption      = 0.0,
      totalChargingEvents   = 0L,
      totalPowerConsumption = 0.0
    )
}
