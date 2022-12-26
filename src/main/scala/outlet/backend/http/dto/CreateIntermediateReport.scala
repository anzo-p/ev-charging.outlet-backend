package outlet.backend.http.dto

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class CreateIntermediateReport(
    outletId: UUID,
    rfidTag: String,
    periodStart: java.time.OffsetDateTime,
    periodEnd: java.time.OffsetDateTime,
    powerConsumption: Double
  )

object CreateIntermediateReport {
  implicit val codec: JsonCodec[CreateIntermediateReport] =
    DeriveJsonCodec.gen[CreateIntermediateReport]
}
