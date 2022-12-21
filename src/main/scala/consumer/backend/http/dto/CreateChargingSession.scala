package consumer.backend.http.dto

import shared.types.TimeExtensions._
import shared.types.enums.{OutletStatus, PurchaseChannel}
import shared.types.{ChargingConsumer, ChargingOutlet, ChargingSession}
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class CreateChargingConsumer(userId: UUID, userToken: Option[String])

object CreateChargingConsumer {
  implicit val codec: JsonCodec[CreateChargingConsumer] = DeriveJsonCodec.gen[CreateChargingConsumer]
}

final case class CreateChargingOutlet(outletId: UUID, deviceCode: String, status: String)

object CreateChargingOutlet {
  implicit val codec: JsonCodec[CreateChargingOutlet] = DeriveJsonCodec.gen[CreateChargingOutlet]
}

final case class CreateChargingSession(
    sessionId: UUID,
    consumer: CreateChargingConsumer,
    outlet: CreateChargingOutlet,
    purchaseChannel: String,
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime]
  ) {

  def toEvent: ChargingSession =
    ChargingSession(
      sessionId       = this.sessionId,
      consumer        = ChargingConsumer(consumer.userId, consumer.userToken),
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
      consumer        = CreateChargingConsumer(event.consumer.userId, event.consumer.userToken),
      outlet          = CreateChargingOutlet(event.outlet.outletId, event.outlet.deviceCode, event.outlet.status.entryName),
      purchaseChannel = event.purchaseChannel.entryName,
      startTime       = event.startTime.toJavaOffsetDateTime,
      endTime         = event.endTime.map(_.toJavaOffsetDateTime)
    )
}
