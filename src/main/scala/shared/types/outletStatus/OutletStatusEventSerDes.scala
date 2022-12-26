package shared.types.outletStatus

import com.anzop.evCharger.outletStatusEvent._
import nl.vroste.zio.kinesis.client.serde.Serde
import nl.vroste.zio.kinesis.client.serde.Serde.bytes
import shared.types.TimeExtensions._
import shared.types.enums.{OutletDeviceState, OutletStateRequester}
import zio.Chunk

import java.util.UUID

object OutletStatusEventSerDes {

  def toProtobuf(session: EventSessionData): EventSessionDataProto =
    EventSessionDataProto(
      rfidTag          = session.rfidTag,
      periodStart      = session.periodStart.map(_.toProtobufTs),
      periodEnd        = session.periodEnd.map(_.toProtobufTs),
      powerConsumption = session.powerConsumption
    )

  def toProtobuf(outlet: OutletStatusEvent): OutletStatusEventProto =
    OutletStatusEventProto(
      requester     = outlet.requester.entryName,
      outletId      = outlet.outletId.toString,
      eventTime     = Some(outlet.eventTime.toProtobufTs),
      state         = outlet.state.entryName,
      recentSession = Some(toProtobuf(outlet.recentSession))
    )

  def fromProtobuf(proto: EventSessionDataProto): EventSessionData =
    EventSessionData(
      sessionId        = Some(UUID.fromString(proto.sessionId)),
      rfidTag          = proto.rfidTag,
      periodStart      = proto.periodStart.map(_.toJavaOffsetDateTime),
      periodEnd        = proto.periodEnd.map(_.toJavaOffsetDateTime),
      powerConsumption = proto.powerConsumption
    )

  def fromProtobuf(proto: OutletStatusEventProto): OutletStatusEvent =
    OutletStatusEvent(
      requester     = OutletStateRequester.withName(proto.requester),
      outletId      = UUID.fromString(proto.outletId),
      eventTime     = proto.eventTime.get.toJavaOffsetDateTime,
      state         = OutletDeviceState.withName(proto.state),
      recentSession = fromProtobuf(proto.recentSession.get)
    )

  val byteArray: Serde[Any, OutletStatusEvent] =
    bytes.inmap(chunk => fromProtobuf(OutletStatusEventProto.parseFrom(chunk.toArray)))(event =>
      Chunk.fromArray(toProtobuf(event).toByteArray))
}
