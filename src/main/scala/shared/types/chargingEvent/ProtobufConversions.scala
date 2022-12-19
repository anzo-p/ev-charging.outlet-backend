package shared.types.chargingEvent

import com.anzop.todo.chargerEvent.{ChargingConsumerProto, ChargingDeviceProto, ChargingEventProto, ChargingSessionProto}
import nl.vroste.zio.kinesis.client.serde.Serde
import nl.vroste.zio.kinesis.client.serde.Serde.bytes
import zio.Chunk

object ProtobufConversions {

  def toProtobuf(consumer: ChargingConsumer): ChargingConsumerProto =
    ChargingConsumerProto(
      consumer.token
    )

  def toProtobuf(device: ChargingDevice): ChargingDeviceProto =
    ChargingDeviceProto(
      device.deviceId,
      device.outletGroupId,
      device.deviceCode
    )

  def toProtobuf(session: ChargingSession): ChargingSessionProto =
    ChargingSessionProto(
      sessionId = session.sessionId,
      startTime = Some(session.startTime),
      endTime   = session.endTime
    )

  def toProtobuf(event: ChargingEvent): ChargingEventProto =
    ChargingEventProto(
      consumer = Some(toProtobuf(event.consumer)),
      device   = Some(toProtobuf(event.device)),
      session  = Some(toProtobuf(event.session))
    )

  def fromProtobuf(proto: ChargingConsumerProto): ChargingConsumer =
    ChargingConsumer(
      proto.token
    )

  def fromProtobuf(proto: ChargingDeviceProto): ChargingDevice =
    ChargingDevice(
      proto.deviceId,
      proto.outletGroupId,
      proto.deviceCode
    )

  def fromProtobuf(proto: ChargingSessionProto): ChargingSession =
    ChargingSession(
      proto.sessionId,
      proto.startTime.get,
      proto.endTime
    )

  def fromProtobuf(proto: ChargingEventProto): ChargingEvent =
    ChargingEvent(
      fromProtobuf(proto.consumer.get),
      fromProtobuf(proto.device.get),
      fromProtobuf(proto.session.get)
    )

  val byteArray: Serde[Any, ChargingEvent] =
    bytes.inmap(chunk => fromProtobuf(ChargingEventProto.parseFrom(chunk.toArray)))(event => Chunk.fromArray(toProtobuf(event).toByteArray))
}
