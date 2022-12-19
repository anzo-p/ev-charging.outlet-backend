package shared.types.chargingEvent

import com.google.protobuf.timestamp.Timestamp

case class ChargingConsumer(token: String)

case class ChargingDevice(deviceId: String, outletGroupId: String, deviceCode: String)

case class ChargingSession(sessionId: String, startTime: Timestamp, endTime: Option[Timestamp])

case class ChargingEvent(consumer: ChargingConsumer, device: ChargingDevice, session: ChargingSession)

object ChargingEvent {

  def apply(
      consumerToken: String,
      deviceId: String,
      outletGroupId: String,
      deviceCode: String,
      sessionId: String,
      startTime: Timestamp,
      endTime: Option[Timestamp]
    ): ChargingEvent =
    ChargingEvent(
      ChargingConsumer(consumerToken),
      ChargingDevice(deviceId, outletGroupId, deviceCode),
      ChargingSession(sessionId, startTime, endTime)
    )
}
