package customer.backend.http.dto

import shared.types.enums.{OutletStatus, PurchaseChannel}
import shared.types.{ChargingCustomer, ChargingOutlet, ChargingSession}
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class CreateChargingCustomer(rfidTag: Option[String])

object CreateChargingCustomer {
  implicit val codec: JsonCodec[CreateChargingCustomer] = DeriveJsonCodec.gen[CreateChargingCustomer]
}

final case class CreateChargingOutlet(outletId: UUID, deviceCode: String, status: String)

object CreateChargingOutlet {
  implicit val codec: JsonCodec[CreateChargingOutlet] = DeriveJsonCodec.gen[CreateChargingOutlet]
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

  def toEvent: ChargingSession =
    ChargingSession(
      sessionId,
      customerId,
      ChargingCustomer(customer.rfidTag),
      ChargingOutlet(outlet.outletId, outlet.deviceCode, OutletStatus.withName(outlet.status)),
      PurchaseChannel.withName(purchaseChannel),
      startTime,
      endTime
    )
}

object CreateChargingSession {
  implicit val codec: JsonCodec[CreateChargingSession] = DeriveJsonCodec.gen[CreateChargingSession]

  def fromEvent(event: ChargingSession): CreateChargingSession =
    CreateChargingSession(
      event.sessionId,
      event.customerId,
      CreateChargingCustomer(event.customer.rfidTag),
      CreateChargingOutlet(event.outlet.outletId, event.outlet.deviceCode, event.outlet.status.entryName),
      event.purchaseChannel.entryName,
      event.startTime,
      event.endTime
    )
}
