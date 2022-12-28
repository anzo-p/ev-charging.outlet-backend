package outlet.backend.types

import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState
import shared.types.enums.OutletDeviceState.isPreStateTo
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class ChargerOutlet(
    outletId: UUID,
    chargerGroupId: UUID,
    outletCode: String,
    address: String,
    maxPower: Double,
    state: OutletDeviceState,
    sessionId: Option[UUID],
    rfidTag: String,
    startTime: Option[java.time.OffsetDateTime],
    endTime: Option[java.time.OffsetDateTime],
    powerConsumption: Double,
    totalChargingEvents: Long,
    totalPowerConsumption: Double
  )

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
      state                 = OutletDeviceState.Available,
      sessionId             = None,
      rfidTag               = "",
      startTime             = None,
      endTime               = None,
      powerConsumption      = 0.0,
      totalChargingEvents   = 0L,
      totalPowerConsumption = 0.0
    )

  def mayTransitionTo(nextState: OutletDeviceState): ChargerOutlet => Boolean =
    _.state.in(isPreStateTo(nextState))
}
