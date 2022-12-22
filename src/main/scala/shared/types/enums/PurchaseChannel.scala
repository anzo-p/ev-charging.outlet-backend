package shared.types.enums

import enumeratum.{Enum, EnumEntry}
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.schema.{DeriveSchema, Schema}

sealed trait PurchaseChannel extends EnumEntry

object PurchaseChannel extends Enum[PurchaseChannel] {
  val values: IndexedSeq[PurchaseChannel] = findValues

  implicit lazy val schema: Schema[PurchaseChannel] =
    DeriveSchema.gen[PurchaseChannel]

  implicit val codec: JsonCodec[PurchaseChannel] =
    DeriveJsonCodec.gen[PurchaseChannel]

  case object OutletDevice extends PurchaseChannel
  case object MobileApp extends PurchaseChannel
}
