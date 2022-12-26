package customer.backend.http.dto

import customer.backend.types.chargingSession.{ChargingCustomer, ChargingOutlet, ChargingSession}
import shared.types.enums.{OutletDeviceState, PurchaseChannel}
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class CreateChargingCustomer(rfidTag: String)

object CreateChargingCustomer {
  implicit val codec: JsonCodec[CreateChargingCustomer] =
    DeriveJsonCodec.gen[CreateChargingCustomer]
}

final case class CreateChargingOutlet(outletId: UUID, deviceCode: String, state: String)

object CreateChargingOutlet {
  implicit val codec: JsonCodec[CreateChargingOutlet] =
    DeriveJsonCodec.gen[CreateChargingOutlet]
}

final case class CreateChargingSession(
    sessionId: UUID,
    customerId: UUID,
    customer: CreateChargingCustomer,
    outlet: CreateChargingOutlet,
    purchaseChannel: String,
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime]
  ) {

  def toModel: ChargingSession =
    ChargingSession(
      sessionId,
      customerId,
      customer = ChargingCustomer(customer.rfidTag),
      outlet   = ChargingOutlet(outlet.outletId, outlet.deviceCode, OutletDeviceState.withName(outlet.state)),
      PurchaseChannel.withName(purchaseChannel),
      startTime,
      endTime
    )
}

object CreateChargingSession {
  implicit val codec: JsonCodec[CreateChargingSession] =
    DeriveJsonCodec.gen[CreateChargingSession]

  def fromModel(event: ChargingSession): CreateChargingSession =
    CreateChargingSession(
      event.sessionId,
      event.customerId,
      customer = CreateChargingCustomer(event.customer.rfidTag),
      outlet   = CreateChargingOutlet(event.outlet.outletId, event.outlet.deviceCode, event.outlet.state.entryName),
      event.purchaseChannel.entryName,
      event.startTime,
      event.endTime
    )
}
