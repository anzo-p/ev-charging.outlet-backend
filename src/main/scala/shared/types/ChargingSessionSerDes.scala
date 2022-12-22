package shared.types

import com.anzop.evCharger.chargingSession._
import nl.vroste.zio.kinesis.client.serde.Serde
import nl.vroste.zio.kinesis.client.serde.Serde.bytes
import shared.types.enums.{OutletStatus, PurchaseChannel}
import zio.Chunk

import java.util.UUID
import scala.util.Try

object ChargingSessionSerDes {

  def toProtobuf(customer: ChargingCustomer): ChargingCustomerProto =
    ChargingCustomerProto(
      customerId = customer.customerId.toString,
      rfidTag    = customer.rfidTag.getOrElse("")
    )

  def toProtobuf(device: ChargingOutlet): ChargingOutletProto =
    ChargingOutletProto(
      outletId   = device.outletId.toString,
      deviceCode = device.deviceCode,
      status     = device.status.entryName
    )

  def toProtobuf(session: ChargingSession): ChargingSessionProto =
    ChargingSessionProto(
      sessionId       = session.sessionId.toString,
      customer        = Some(toProtobuf(session.customer)),
      outlet          = Some(toProtobuf(session.outlet)),
      purchaseChannel = session.purchaseChannel.entryName,
      startTime       = Some(session.startTime),
      endTime         = session.endTime
    )

  def fromProtobuf(proto: ChargingCustomerProto): ChargingCustomer =
    ChargingCustomer(
      customerId = UUID.fromString(proto.customerId),
      rfidTag    = if (proto.rfidTag.isEmpty) None else Some(proto.rfidTag)
    )

  def fromProtobuf(proto: ChargingOutletProto): ChargingOutlet =
    ChargingOutlet(
      outletId   = UUID.fromString(proto.outletId),
      deviceCode = proto.deviceCode,
      status     = OutletStatus.withName(proto.status)
    )

  def fromProtobuf(proto: ChargingSessionProto): ChargingSession =
    Try {
      ChargingSession(
        sessionId       = UUID.fromString(proto.sessionId),
        customer        = fromProtobuf(proto.customer.get),
        outlet          = fromProtobuf(proto.outlet.get),
        purchaseChannel = PurchaseChannel.withName(proto.purchaseChannel),
        startTime       = proto.startTime.get,
        endTime         = proto.endTime
      )
    } match {
      case scala.util.Success(value) => value
      case scala.util.Failure(exception) =>
        println(exception)
        throw exception
    }

  val byteArray: Serde[Any, ChargingSession] =
    bytes.inmap(chunk => fromProtobuf(ChargingSessionProto.parseFrom(chunk.toArray)))(event =>
      Chunk.fromArray(toProtobuf(event).toByteArray))
}
