package outlet.backend.types.chargerOutlet

import outlet.backend.http.dto.ChargerOutletDto
import shared.types.enums.OutletDeviceState

import java.util.UUID

object Fixtures {

  val fixtureBasicChargerOutlet: ChargerOutlet =
    new ChargerOutlet(
      outletId              = UUID.randomUUID(),
      chargerGroupId        = UUID.randomUUID(),
      outletCode            = "ABC123",
      address               = "I think were lost or forgotten",
      maxPower              = 22.0,
      outletState           = OutletDeviceState.Available,
      sessionId             = None,
      rfidTag               = "",
      startTime             = java.time.OffsetDateTime.now().minusDays(1L),
      endTime               = None,
      powerConsumption      = 0,
      totalChargingEvents   = 0L,
      totalPowerConsumption = 0.0
    )

  val fixtureChargerOutletDto: ChargerOutletDto =
    ChargerOutletDto(
      outletId       = None,
      chargerGroupId = fixtureBasicChargerOutlet.chargerGroupId,
      outletCode     = fixtureBasicChargerOutlet.outletCode,
      address        = fixtureBasicChargerOutlet.address,
      maxPower       = fixtureBasicChargerOutlet.maxPower,
      outletState    = fixtureBasicChargerOutlet.outletState.entryName
    )
}
