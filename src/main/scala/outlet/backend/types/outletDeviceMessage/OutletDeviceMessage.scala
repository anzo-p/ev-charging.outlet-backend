package outlet.backend.types.outletDeviceMessage

import shared.types.chargingEvent.{ChargingEvent, EventSession}
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio.sqs.serialization.Serializer

import java.util.UUID

final case class OutletDeviceMessage(
    outletId: UUID,
    rfidTag: String,
    periodStart: java.time.OffsetDateTime,
    periodEnd: java.time.OffsetDateTime,
    outletStatus: OutletDeviceState,
    powerConsumption: Double
  ) {

  def toChargingEvent: ChargingEvent =
    ChargingEvent(
      initiator   = EventInitiator.OutletBackend,
      outletId    = this.outletId,
      outletState = this.outletStatus,
      recentSession = EventSession(
        sessionId        = None,
        rfidTag          = this.rfidTag,
        periodStart      = this.periodStart,
        periodEnd        = Some(this.periodEnd),
        powerConsumption = this.powerConsumption
      )
    )
}

object OutletDeviceMessage extends Serializer[OutletDeviceMessage] {
  import zio.json.{DecoderOps, DeriveJsonCodec, EncoderOps, JsonCodec}

  implicit val codec: JsonCodec[OutletDeviceMessage] = DeriveJsonCodec.gen[OutletDeviceMessage]

  override def apply(message: OutletDeviceMessage): String =
    message.toJson

  def unapply(message: String): Either[String, OutletDeviceMessage] =
    message.fromJson[OutletDeviceMessage]

  def fromChargingEvent(event: ChargingEvent): OutletDeviceMessage =
    OutletDeviceMessage(
      outletId         = event.outletId,
      rfidTag          = event.recentSession.rfidTag,
      periodStart      = event.recentSession.periodStart,
      periodEnd        = event.recentSession.periodEnd.getOrElse(java.time.OffsetDateTime.now()),
      outletStatus     = event.outletState,
      powerConsumption = event.recentSession.powerConsumption
    )
}
