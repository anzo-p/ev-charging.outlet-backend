package outlet.backend.services

import outlet.backend.ChargerOutletService
import outlet.backend.types.ChargerOutlet
import ChargerOutlet.mayTransitionTo
import shared.db.DynamoDBPrimitives
import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState
import shared.types.outletStatus.OutletStatusEvent
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb._
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

final case class DynamoDBChargerOutletService(executor: DynamoDBExecutor)
    extends ChargerOutletService
    with DynamoDBPrimitives[ChargerOutlet]
    with DateTimeSchemaImplicits {

  val tableResource = "ev-outlet-app.charger-outlet.table"
  val primaryKey    = "outletId"

  override def schema: Schema[ChargerOutlet] = DeriveSchema.gen[ChargerOutlet]

  private def cannotTransitionTo(targetState: OutletDeviceState): String =
    s"outlet not in (one of) state(s) ${OutletDeviceState.getPreStatesTo(targetState).mkString("[ ", " ,", " ]")}"

  private def checkAndSetState(
      outletId: UUID,
      nextState: OutletDeviceState,
      message: String      = "no data found",
      yieldResult: Boolean = false
    ): ZIO[Any, Throwable, Option[ChargerOutlet]] =
    (for {
      data    <- getByPK(outletId).filterOrFail(mayTransitionTo(nextState))(new Error(message))
      updated <- ZIO.succeed(data.copy(state = nextState))
      _       <- putByPK(updated)
    } yield if (yieldResult) Some(updated) else None)
      .provideLayer(ZLayer.succeed(executor))

  override def register(outlet: ChargerOutlet): Task[ChargerOutlet] =
    (for {
      inserted <- ZIO.succeed(outlet)
      _        <- put(tableResource, inserted).execute
    } yield inserted)
      .provideLayer(ZLayer.succeed(executor))

  override def setAvailable(outletId: UUID): Task[Unit] =
    checkAndSetState(
      outletId,
      OutletDeviceState.Available,
      cannotTransitionTo(OutletDeviceState.Available)
    ).unit

  override def setCablePlugged(outletId: UUID): Task[Unit] =
    checkAndSetState(
      outletId,
      OutletDeviceState.CablePlugged,
      cannotTransitionTo(OutletDeviceState.CablePlugged)
    ).unit

  override def setChargingRequested(outletId: UUID, rfidTag: String): ZIO[Any, Throwable, ChargerOutlet] = {
    val targetState = OutletDeviceState.Charging

    checkAndSetState(
      outletId,
      targetState,
      cannotTransitionTo(targetState),
      yieldResult = true
    ).flatMap {
      case None =>
        ZIO.fail(new Error("unsuccessful update"))
      case Some(outlet) =>
        ZIO.succeed(outlet)
    }
  }

  override def beginCharging(outletId: UUID): Task[Unit] =
    checkAndSetState(outletId, OutletDeviceState.Charging).unit

  override def aggregateConsumption(status: OutletStatusEvent): Task[ChargerOutlet] = {
    val targetState = OutletDeviceState.Charging

    (for {
      data <- getByPK(status.outletId).filterOrFail(mayTransitionTo(targetState))(new Error(cannotTransitionTo(targetState)))

      update <- ZIO.succeed(
                 data.copy(
                   endTime          = status.recentSession.periodEnd,
                   powerConsumption = data.powerConsumption + status.recentSession.powerConsumption))

      _ <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))
  }

  override def stopCharging(status: OutletStatusEvent): Task[ChargerOutlet] = {
    val targetState = OutletDeviceState.CablePlugged

    (for {
      data <- getByPK(status.outletId).filterOrFail(mayTransitionTo(targetState))(new Error(cannotTransitionTo(targetState)))

      update <- ZIO.succeed(
                 data.copy(
                   state            = OutletDeviceState.CablePlugged,
                   endTime          = status.recentSession.periodEnd,
                   powerConsumption = data.powerConsumption + status.recentSession.powerConsumption
                 ))

      _ <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))
  }
}

object DynamoDBChargerOutletService {

  val live: ZLayer[DynamoDBExecutor, Nothing, ChargerOutletService] =
    ZLayer.fromFunction(DynamoDBChargerOutletService.apply _)
}
