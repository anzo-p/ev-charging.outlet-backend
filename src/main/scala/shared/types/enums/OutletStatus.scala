package shared.types.enums

import enumeratum.{Enum, EnumEntry}
import zio.json.{DeriveJsonCodec, JsonCodec}

sealed trait OutletStatus extends EnumEntry

object OutletStatus extends Enum[OutletStatus] {
  val values: IndexedSeq[OutletStatus] = findValues

  implicit val codec: JsonCodec[OutletStatus] =
    DeriveJsonCodec.gen[OutletStatus]

  case object Available extends OutletStatus
  case object Reserved extends OutletStatus
  case object ChargingRequested extends OutletStatus
  case object StoppingRequested extends OutletStatus
  case class Broken(reason: String) extends OutletStatus
}
