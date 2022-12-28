package customer.backend.types.chargingSession

import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.{OutletDeviceState, PurchaseChannel}
import shared.types.outletStatus.OutletStatusEvent
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class ChargingSession(
    sessionId: UUID,
    customerId: UUID,
    rfidTag: String,
    outletId: UUID,
    sessionState: OutletDeviceState,
    purchaseChannel: PurchaseChannel,
    startTime: Option[java.time.OffsetDateTime],
    endTime: Option[java.time.OffsetDateTime],
    powerConsumption: Option[Double]
  )

object ChargingSession extends DateTimeSchemaImplicits {

  implicit lazy val schema: Schema[ChargingSession] =
    DeriveSchema.gen[ChargingSession]

  def apply(customerId: UUID, outletId: UUID, purchaseChannel: PurchaseChannel): ChargingSession =
    ChargingSession(
      sessionId        = UUID.randomUUID(),
      customerId       = customerId,
      rfidTag          = "",
      outletId         = outletId,
      sessionState     = OutletDeviceState.ChargingRequested,
      purchaseChannel  = purchaseChannel,
      startTime        = None,
      endTime          = None,
      powerConsumption = Some(0.0)
    )

  def fromEvent(customerId: UUID, event: OutletStatusEvent): ChargingSession =
    ChargingSession(
      sessionId        = event.recentSession.sessionId.getOrElse(UUID.randomUUID()),
      customerId       = customerId,
      rfidTag          = event.recentSession.rfidTag,
      outletId         = event.outletId,
      sessionState     = event.state,
      purchaseChannel  = PurchaseChannel.OutletDevice,
      startTime        = event.recentSession.periodStart,
      endTime          = event.recentSession.periodEnd,
      powerConsumption = Some(event.recentSession.powerConsumption)
    )
}

final case class ChargingSessionUpdate(
    sessionId: UUID,
    rfidTag: String,
    outletId: UUID,
    state: OutletDeviceState,
    startTime: java.time.OffsetDateTime,
    endTime: java.time.OffsetDateTime,
    powerConsumption: Double
  )

object ChargingSessionUpdate extends DateTimeSchemaImplicits {

  implicit lazy val schema: Schema[ChargingSessionUpdate] =
    DeriveSchema.gen[ChargingSessionUpdate]

  def fromEvent(
      event: OutletStatusEvent,
      sessionId: UUID,
      startTime: java.time.OffsetDateTime,
      endTime: java.time.OffsetDateTime
    ): ChargingSessionUpdate =
    ChargingSessionUpdate(
      sessionId        = sessionId,
      rfidTag          = event.recentSession.rfidTag,
      outletId         = event.outletId,
      state            = event.state,
      startTime        = startTime,
      endTime          = endTime,
      powerConsumption = event.recentSession.powerConsumption
    )
}

/*
  http
  - start
    - creates a ChargingSession and stores in DB
    - creates an OutletStatusEvent with ChargingRequested and writes to kinesis

  - stop
    - updates ChargingSession to completed in DB
    - creates an OutletStatusEvent with StoppingRequested and writes to kinesis

  - get current session report
    - fetch and respond ChargingSession with whatever data it has

  kinesis consumer
  - ack to start - same as intermediate report
    - updates ChargingSession to Charging in DB

  - request to start
    - creates a ChargingSession and stores in DB
    - creates an OutletStatusEvent with Charging and writes to kinesis

  - ack to stop
    - updates ChargingSession to completed in DB




  kinesis consumer
  - ack to start        -  Charging
  - request to start       ChargingRequested
  - intermediate report    Charging
  - ack to stop            CablePlugged | Available

  - request to stop   not available, we may send to device but they'll stop instantly on token request
 */
