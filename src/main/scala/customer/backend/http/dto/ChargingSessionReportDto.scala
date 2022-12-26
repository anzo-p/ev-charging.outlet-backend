package customer.backend.http.dto

import outlet.backend.types.chargerOutlet.ChargerOutlet
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class ChargingSessionReportDto(
    sessionId: Option[UUID],
    outletId: UUID,
    outletCode: String,
    rfidTag: String,
    startTime: Option[java.time.OffsetDateTime],
    endTime: Option[java.time.OffsetDateTime],
    accumulatedPowerConsumption: Double
  )

object ChargingSessionReportDto {
  implicit val codec: JsonCodec[ChargingSessionReportDto] =
    DeriveJsonCodec.gen[ChargingSessionReportDto]

  def fromModel(model: ChargerOutlet): ChargingSessionReportDto =
    ChargingSessionReportDto(
      model.sessionId,
      model.outletId,
      model.outletCode,
      model.rfidTag,
      model.startTime,
      model.endTime,
      model.totalPowerConsumption
    )
}
