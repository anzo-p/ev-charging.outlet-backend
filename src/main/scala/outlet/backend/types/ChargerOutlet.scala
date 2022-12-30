package outlet.backend.types

import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState.getPreStatesTo
import shared.types.enums.{OutletDeviceState, OutletStateRequester}
import shared.types.outletStatus.{EventSessionData, OutletStatusEvent}
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
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime],
    powerConsumption: Double,
    totalChargingEvents: Long,
    totalPowerConsumption: Double
  ) {

  def toOutletStatus: OutletStatusEvent =
    OutletStatusEvent(
      requester = OutletStateRequester.OutletDevice,
      outletId  = this.outletId,
      state     = this.state,
      recentSession = EventSessionData(
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
      state                 = OutletDeviceState.Available,
      sessionId             = None,
      rfidTag               = "",
      startTime             = java.time.OffsetDateTime.now(),
      endTime               = None,
      powerConsumption      = 0.0,
      totalChargingEvents   = 0L,
      totalPowerConsumption = 0.0
    )

  object Ops {
    implicit class ChargerOutletOps(outlet: ChargerOutlet) {

      def mayTransitionTo(targetState: OutletDeviceState): Boolean =
        outlet.state.in(getPreStatesTo(targetState))
    }

    def cannotTransitionTo(targetState: OutletDeviceState): String =
      s"outlet not in (one of) state(s) ${OutletDeviceState.getPreStatesTo(targetState).mkString("[ ", " ,", " ]")}"
  }
}
