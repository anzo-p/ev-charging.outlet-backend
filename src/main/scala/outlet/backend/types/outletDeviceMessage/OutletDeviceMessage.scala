package outlet.backend.types.outletDeviceMessage

import shared.types.chargingEvent.{ChargingEvent, EventSession}
import shared.types.enums.{EventInitiator, OutletDeviceState}
import zio.sqs.serialization.Serializer

import java.util.UUID

/*
{
  "outletId": "0b36a6d2-2694-4ff3-8324-9a391d2a323b",
  "rfidTag": "f23fee8b-79d9-4723-8c03-c2c85a8a06c8",
  "periodStart": "2023-01-05T13:14:19.074Z",
  "periodEnd": "2023-01-05T13:14:19.074Z",
  "outletStatus": "ChargingRequested",
  "powerConsumption": 0.0
}
 */

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
      initiator   = EventInitiator.OutletDevice,
      outletId    = this.outletId,
      outletState = this.outletStatus,
      recentSession = EventSession(
        sessionId        = None, // the outlets know nothing about a session, but the dbs will
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
