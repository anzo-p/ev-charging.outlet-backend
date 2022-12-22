package customer.backend.http.dto

import shared.types.TimeExtensions._
import shared.types.enums.{OutletStatus, PurchaseChannel}
import shared.types.{ChargingCustomer, ChargingOutlet, ChargingSession}
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class CreateChargingCustomer(customerId: UUID, rfidTag: Option[String])

object CreateChargingCustomer {
  implicit val codec: JsonCodec[CreateChargingCustomer] = DeriveJsonCodec.gen[CreateChargingCustomer]
}

final case class CreateChargingOutlet(outletId: UUID, deviceCode: String, status: String)

object CreateChargingOutlet {
  implicit val codec: JsonCodec[CreateChargingOutlet] = DeriveJsonCodec.gen[CreateChargingOutlet]
}

final case class CreateChargingSession(
    sessionId: UUID,
    customer: CreateChargingCustomer,
    outlet: CreateChargingOutlet,
    purchaseChannel: String,
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime]
  ) {

  def toEvent: ChargingSession =
    ChargingSession(
      sessionId       = this.sessionId,
      customer        = ChargingCustomer(customer.customerId, customer.rfidTag),
      outlet          = ChargingOutlet(outlet.outletId, outlet.deviceCode, OutletStatus.withName(outlet.status)),
      purchaseChannel = PurchaseChannel.withName(purchaseChannel),
      startTime       = this.startTime.toProtobufTs,
      endTime         = this.endTime.map(_.toProtobufTs)
    )
}

object CreateChargingSession {
  implicit val codec: JsonCodec[CreateChargingSession] = DeriveJsonCodec.gen[CreateChargingSession]

  def fromEvent(event: ChargingSession): CreateChargingSession =
    CreateChargingSession(
      sessionId       = event.sessionId,
      customer        = CreateChargingCustomer(event.customer.customerId, event.customer.rfidTag),
      outlet          = CreateChargingOutlet(event.outlet.outletId, event.outlet.deviceCode, event.outlet.status.entryName),
      purchaseChannel = event.purchaseChannel.entryName,
      startTime       = event.startTime.toJavaOffsetDateTime,
      endTime         = event.endTime.map(_.toJavaOffsetDateTime)
    )
}
