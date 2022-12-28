package outlet.backend.http.dto

import shared.types.enums.{OutletDeviceState, OutletStateRequester}
import shared.types.outletStatus.{EventSessionData, OutletStatusEvent}
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

  def toOutletStatus(report: CreateIntermediateReport): OutletStatusEvent =
    OutletStatusEvent(
      requester = OutletStateRequester.OutletDevice,
      outletId  = report.outletId,
      eventTime = java.time.OffsetDateTime.now(),
      state     = OutletDeviceState.Charging,
      recentSession = EventSessionData(
        sessionId        = None,
        rfidTag          = report.rfidTag,
        periodStart      = Some(report.periodStart),
        periodEnd        = Some(report.periodEnd),
        powerConsumption = report.powerConsumption
      )
    )
}
