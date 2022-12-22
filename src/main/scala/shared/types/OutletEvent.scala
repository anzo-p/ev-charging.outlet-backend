package shared.types

import com.google.protobuf.timestamp.Timestamp
import shared.types.enums.OutletStatus

import java.util.UUID

final case class OutletEvent(
    outletId: UUID,
    userToken: String,
    eventTime: Timestamp,
    status: OutletStatus
  )

object OutletEvent {

  def apply(
      outletId: UUID,
      userToken: String,
      timestamp: Timestamp,
      status: String
    ): OutletEvent =
    OutletEvent(
      outletId,
      userToken,
      timestamp,
      OutletStatus.withName(status)
    )
}
