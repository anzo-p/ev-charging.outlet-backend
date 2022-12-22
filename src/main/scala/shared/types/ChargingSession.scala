package shared.types

import com.google.protobuf.timestamp.Timestamp
import shared.types.enums.{OutletStatus, PurchaseChannel}

import java.util.UUID

final case class ChargingCustomer(customerId: UUID, rfidTag: Option[String])

final case class ChargingOutlet(outletId: UUID, deviceCode: String, status: OutletStatus)

final case class ChargingSession(
    sessionId: UUID,
    customer: ChargingCustomer,
    outlet: ChargingOutlet,
    purchaseChannel: PurchaseChannel,
    startTime: Timestamp,
    endTime: Option[Timestamp]
  )

object ChargingSession {

  def apply(
      sessionId: UUID,
      customer: ChargingCustomer,
      outlet: ChargingOutlet,
      purchaseChannel: String,
      startTime: Timestamp,
      endTime: Option[Timestamp]
    ): ChargingSession =
    ChargingSession(
      sessionId       = sessionId,
      customer        = customer,
      outlet          = outlet,
      purchaseChannel = PurchaseChannel.withName(purchaseChannel),
      startTime       = startTime,
      endTime         = endTime
    )
}
