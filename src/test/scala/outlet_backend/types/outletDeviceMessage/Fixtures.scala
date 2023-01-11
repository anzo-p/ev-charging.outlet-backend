package outlet_backend.types.outletDeviceMessage

import shared.types.enums.OutletDeviceState

import java.util.UUID

object Fixtures {

  val fixtureBasicDeviceMessage = new OutletDeviceMessage(
    outletId          = UUID.randomUUID(),
    rfidTag           = "AAA111",
    periodStart       = java.time.OffsetDateTime.now(),
    periodEnd         = java.time.OffsetDateTime.now(),
    outletStateChange = OutletDeviceState.Available,
    powerConsumption  = 0.0
  )
}
