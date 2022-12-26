package shared.types.enums

import enumeratum.{Enum, EnumEntry}
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.schema.Schema

sealed trait OutletDeviceState extends EnumEntry

sealed trait AppRequests

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
  case object ChargingRequested extends OutletDeviceState with AppRequests
  case object Charging extends OutletDeviceState
  case object StoppingRequested extends OutletDeviceState with AppRequests
  case object Broken extends OutletDeviceState
}
