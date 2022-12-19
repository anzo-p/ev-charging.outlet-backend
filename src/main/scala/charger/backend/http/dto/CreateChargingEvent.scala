package charger.backend.http.dto

import shared.types.TimeExtensions._
import shared.types.chargingEvent._
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class CreateChargingConsumer(token: String)

object CreateChargingConsumer {
  implicit val codec: JsonCodec[CreateChargingConsumer] = DeriveJsonCodec.gen[CreateChargingConsumer]
}

final case class CreateChargingDevice(deviceId: String, outletGroupId: String, deviceCode: String)

object CreateChargingDevice {
  implicit val codec: JsonCodec[CreateChargingDevice] = DeriveJsonCodec.gen[CreateChargingDevice]
}

final case class CreateChargingSession(
    sessionId: Option[String],
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime]
  )

object CreateChargingSession {
  implicit val codec: JsonCodec[CreateChargingSession] = DeriveJsonCodec.gen[CreateChargingSession]
}

final case class CreateChargingEvent(consumer: CreateChargingConsumer, device: CreateChargingDevice, session: CreateChargingSession) {

  def toEvent: ChargingEvent = {
    ChargingEvent(
      consumer = ChargingConsumer(token  = consumer.token),
      device   = ChargingDevice(deviceId = device.deviceId, outletGroupId = device.outletGroupId, deviceCode = device.deviceCode),
      session = ChargingSession(
        sessionId = session.sessionId.getOrElse(""),
        startTime = session.startTime.toProtobufTs,
        endTime   = session.endTime.map(_.toProtobufTs))
    )
  }
}

object CreateChargingEvent {
  implicit val codec: JsonCodec[CreateChargingEvent] = DeriveJsonCodec.gen[CreateChargingEvent]

  def fromEvent(event: ChargingEvent): CreateChargingEvent = {
    CreateChargingEvent(
      consumer = CreateChargingConsumer(token = event.consumer.token),
      device = CreateChargingDevice(
        deviceId      = event.device.deviceId,
        outletGroupId = event.device.outletGroupId,
        deviceCode    = event.device.deviceCode),
      session = CreateChargingSession(
        sessionId = Some(event.session.sessionId),
        startTime = event.session.startTime.toJavaOffsetDateTime,
        endTime   = event.session.endTime.map(_.toJavaOffsetDateTime)
      )
    )
  }
}
