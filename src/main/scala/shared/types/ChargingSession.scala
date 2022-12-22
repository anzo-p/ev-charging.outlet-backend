package shared.types

import shared.types.enums.{OutletStatus, PurchaseChannel}
import zio.schema.Schema.Field
import zio.schema.{DeriveSchema, Schema, StandardType}

import java.time.format.DateTimeFormatter
import java.util.UUID

final case class ChargingCustomer(rfidTag: Option[String])

final case class ChargingOutlet(outletId: UUID, deviceCode: String, status: OutletStatus)

final case class ChargingSession(
    sessionId: UUID,
    customerId: UUID,
    customer: ChargingCustomer,
    outlet: ChargingOutlet,
    purchaseChannel: PurchaseChannel,
    startTime: java.time.OffsetDateTime,
    endTime: Option[java.time.OffsetDateTime]
  )

object ChargingSession {

  implicit val offsetDateTimeSchema: Schema[java.time.OffsetDateTime] =
    Schema.Primitive(StandardType.OffsetDateTimeType(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

  implicit val offsetDateTimeField: Field[java.time.OffsetDateTime] =
    Schema.Field[java.time.OffsetDateTime]("offsetDateTimeField", offsetDateTimeSchema)

  implicit lazy val schema: Schema[ChargingSession] = DeriveSchema.gen[ChargingSession]

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
