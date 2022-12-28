package shared.types.enums

import enumeratum.{Enum, EnumEntry}
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.schema.Schema

import scala.collection.mutable

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
  case object Broken extends OutletDeviceState
  case object CablePlugged extends OutletDeviceState
  case object ChargingRequested extends OutletDeviceState with AppState
  case object Charging extends OutletDeviceState with AppState
  case object StoppingRequested extends OutletDeviceState with AppState
  case object Finished extends OutletDeviceState with AppState

  private object Transitions {

    val allowedTransitions: Map[OutletDeviceState, Seq[OutletDeviceState]] =
      Map(
        Available         -> Seq(CablePlugged),
        CablePlugged      -> Seq(Available, Charging, ChargingRequested),
        ChargingRequested -> Seq(CablePlugged, Charging),
        Charging          -> Seq(Charging, CablePlugged, StoppingRequested, Finished),
        StoppingRequested -> Seq(CablePlugged, Finished),
        Finished          -> Seq(Available)
      )

    val preStates: mutable.HashMap[OutletDeviceState, Seq[OutletDeviceState]] = mutable.HashMap()

    allowedTransitions.keys.foreach { key =>
      allowedTransitions.keys.foreach { value =>
        if (allowedTransitions(key).contains(value)) {
          if (preStates.contains(value)) {
            preStates.put(value, preStates(value) :+ key)
          }
          else {
            preStates.put(value, Seq(key))
          }
        }
      }
    }
  }

  def isPreStateTo(next: OutletDeviceState): Seq[OutletDeviceState] =
    OutletDeviceState.Transitions.preStates(next)
}
