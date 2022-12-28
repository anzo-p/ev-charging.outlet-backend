package shared.types.enums

import enumeratum.{Enum, EnumEntry}
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.schema.Schema

sealed trait OutletDeviceState extends EnumEntry

sealed trait AppState

object OutletDeviceState extends Enum[OutletDeviceState] {
  val values: IndexedSeq[OutletDeviceState] = findValues

  implicit val codec: JsonCodec[OutletDeviceState] =
    DeriveJsonCodec.gen[OutletDeviceState]

  implicit val schema: Schema[OutletDeviceState] =
    Schema[String].transform(
      OutletDeviceState.withName,
      _.entryName
    )

  case object Available extends OutletDeviceState
  case object CablePlugged extends OutletDeviceState
  case object ChargingRequested extends OutletDeviceState with AppState
  case object Charging extends OutletDeviceState with AppState
  case object StoppingRequested extends OutletDeviceState with AppState
  case object Broken extends OutletDeviceState
}
