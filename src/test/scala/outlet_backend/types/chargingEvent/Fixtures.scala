package outlet_backend.types.chargingEvent

import shared.types.chargingEvent.{ChargingEvent, EventSession}
import shared.types.enums.{EventInitiator, OutletDeviceState}

import java.util.UUID

object Fixtures {

  val fixtureBasicChargingEvent: ChargingEvent =
    ChargingEvent(
      initiator   = EventInitiator.OutletBackend,
      outletId    = UUID.randomUUID(),
      outletState = OutletDeviceState.Available,
      recentSession = EventSession(
        sessionId        = None,
        rfidTag          = "",
        periodStart      = java.time.OffsetDateTime.now().minusMinutes(5L),
        periodEnd        = None,
        powerConsumption = 0.0
      )
    )
}
