package shared.types

import com.anzop.evCharger.outletEvent._
import nl.vroste.zio.kinesis.client.serde.Serde
import nl.vroste.zio.kinesis.client.serde.Serde.bytes
import shared.types.enums.OutletStatus
import zio.Chunk

import java.util.UUID

object OutletEventSerDes {

  def toProtobuf(outlet: OutletEvent): OutletEventProto =
    OutletEventProto(
      outletId  = outlet.outletId.toString,
      rfidTag   = outlet.userToken,
      eventTime = Some(outlet.eventTime),
      status    = outlet.status.entryName
    )

  def fromProtobuf(proto: OutletEventProto): OutletEvent = {
    OutletEvent(
      outletId  = UUID.fromString(proto.outletId),
      userToken = proto.rfidTag,
      eventTime = proto.eventTime.get,
      status    = OutletStatus.withName(proto.status)
    )
  }

  val byteArray: Serde[Any, OutletEvent] =
    bytes.inmap(chunk => fromProtobuf(OutletEventProto.parseFrom(chunk.toArray)))(event => Chunk.fromArray(toProtobuf(event).toByteArray))
}
