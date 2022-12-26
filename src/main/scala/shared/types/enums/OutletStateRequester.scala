package shared.types.enums

import enumeratum.{Enum, EnumEntry}
import zio.json.{DeriveJsonCodec, JsonCodec}

sealed trait OutletStateRequester extends EnumEntry

object OutletStateRequester extends Enum[OutletStateRequester] {
  val values: IndexedSeq[OutletStateRequester] = findValues

  implicit val codec: JsonCodec[OutletStateRequester] =
    DeriveJsonCodec.gen[OutletStateRequester]

  case object Application extends OutletStateRequester
  case object OutletDevice extends OutletStateRequester
}
