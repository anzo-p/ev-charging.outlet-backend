package customer.backend.types.chargingSession

import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.{OutletDeviceState, PurchaseChannel}
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class ChargingCustomer(rfidTag: String) // not needed

final case class ChargingOutlet(outletId: UUID, deviceCode: String, state: OutletDeviceState) // state not needed

final case class ChargingSession(
    sessionId: UUID,
    customerId: UUID,
    customer: ChargingCustomer,
    outlet: ChargingOutlet,
    purchaseChannel: PurchaseChannel,
    // latestUpdate ts
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime]
    // powerConsumption: Double
  )

object ChargingSession extends DateTimeSchemaImplicits {

  implicit lazy val schema: Schema[ChargingSession] =
    DeriveSchema.gen[ChargingSession]

  def apply(
      sessionId: UUID,
      customerId: UUID,
      customer: ChargingCustomer,
      outlet: ChargingOutlet,
      purchaseChannel: String,
      startTime: java.time.OffsetDateTime,
      endTime: Option[java.time.OffsetDateTime]
    ): ChargingSession =
    ChargingSession(
      sessionId,
      customerId,
      customer,
      outlet,
      PurchaseChannel.withName(purchaseChannel),
      startTime,
      endTime
    )
}
/*
  ChargingSession must be made one-dimensional for dynamodb
  consolidate logics on already-alert
 */

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
