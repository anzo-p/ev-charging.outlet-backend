package shared.types

import com.google.protobuf.timestamp.Timestamp
import shared.types.enums.{OutletStatus, PurchaseChannel}

import java.util.UUID

final case class ChargingConsumer(userId: UUID, userToken: Option[String])

final case class ChargingOutlet(outletId: UUID, deviceCode: String, status: OutletStatus)

final case class ChargingSession(
    sessionId: UUID,
    consumer: ChargingConsumer,
    outlet: ChargingOutlet,
    purchaseChannel: PurchaseChannel,
    startTime: Timestamp,
    endTime: Option[Timestamp]
  )

object ChargingSession {

  def apply(
      sessionId: UUID,
      consumer: ChargingConsumer,
      outlet: ChargingOutlet,
      purchaseChannel: String,
      startTime: Timestamp,
      endTime: Option[Timestamp]
    ): ChargingSession =
    ChargingSession(
      sessionId       = sessionId,
      consumer        = consumer,
      outlet          = outlet,
      purchaseChannel = PurchaseChannel.withName(purchaseChannel),
      startTime       = startTime,
      endTime         = endTime
    )
}
