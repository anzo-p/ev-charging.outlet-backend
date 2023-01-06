package outlet.backend.http.dto

import outlet.backend.types.chargerOutlet.ChargerOutlet
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class ChargerOutletDto(
    outletId: Option[UUID],
    chargerGroupId: UUID,
    outletCode: String,
    address: String,
    maxPower: Double,
    outletState: String
  ) {

  def toModel: ChargerOutlet =
    ChargerOutlet(
      chargerGroupId,
      outletCode,
      address,
      maxPower
    )
}

object ChargerOutletDto {
  implicit val codec: JsonCodec[ChargerOutletDto] = DeriveJsonCodec.gen[ChargerOutletDto]

  def fromModel(model: ChargerOutlet): ChargerOutletDto =
    ChargerOutletDto(
      Some(model.outletId),
      model.chargerGroupId,
      model.outletCode,
      model.address,
      model.maxPower,
      model.outletState.entryName
    )
}
