package outlet.backend.http.dto

import outlet.backend.types.chargerOutlet.ChargerOutlet
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

final case class CreateChargerOutletDto(
    outletId: Option[UUID],
    chargerGroupId: UUID,
    outletCode: String,
    address: String,
    maxPower: Double,
    state: String
  ) {

  def toModel: ChargerOutlet =
    ChargerOutlet(
      chargerGroupId,
      outletCode,
      address,
      maxPower
    )
}

object CreateChargerOutletDto {
  implicit val codec: JsonCodec[CreateChargerOutletDto] = DeriveJsonCodec.gen[CreateChargerOutletDto]

  def fromModel(model: ChargerOutlet): CreateChargerOutletDto =
    CreateChargerOutletDto(
      Some(model.outletId),
      model.chargerGroupId,
      model.outletCode,
      model.address,
      model.maxPower,
      model.state.entryName
    )
}
