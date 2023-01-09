package outlet.backend.types.chargerOutlet

import outlet.backend.http.dto.ChargerOutletDto
import shared.types.enums.OutletDeviceState

import java.util.UUID

object ChargerOutletSpec {

  val testChargerOutletDto =
    ChargerOutletDto(
      outletId       = None,
      chargerGroupId = UUID.fromString("0b36a6d2-2694-4ff3-8324-9a391d2a323b"),
      outletCode     = "12345",
      address        = "nowhere",
      maxPower       = 22.000,
      outletState    = OutletDeviceState.Available.entryName
    )

  val testChargerOutlet =
    testChargerOutletDto.toModel
}
