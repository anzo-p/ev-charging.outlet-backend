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

  private def checkAndSetState(
      outletId: UUID,
      nextState: OutletDeviceState,
      yieldResult: Boolean = false
    ): ZIO[Any, Throwable, Option[ChargerOutlet]] =
    (for {
      data    <- getByPK(outletId).filterOrDie(mayTransitionTo(nextState))(new Error("no data found"))
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
    checkAndSetState(outletId, OutletDeviceState.Available).unit

  override def setCablePlugged(outletId: UUID): Task[Unit] =
    checkAndSetState(outletId, OutletDeviceState.CablePlugged).unit

  override def setChargingRequested(outletId: UUID, rfidTag: String): ZIO[Any, Throwable, ChargerOutlet] =
    checkAndSetState(outletId, OutletDeviceState.ChargingRequested, yieldResult = true).flatMap {
      case None =>
        ZIO.fail(new Error("unsuccessful update"))
      case Some(outlet) =>
        ZIO.succeed(outlet)
    }

  override def beginCharging(outletId: UUID): Task[Unit] =
    checkAndSetState(outletId, OutletDeviceState.Charging).unit

  override def aggregateConsumption(status: OutletStatusEvent): Task[ChargerOutlet] =
    (for {
      data <- getByPK(status.outletId).filterOrDie(mayTransitionTo(OutletDeviceState.Charging))(new Error("no data found"))

      update <- ZIO.succeed(
                 data.copy(
                   endTime          = status.recentSession.periodEnd,
                   powerConsumption = data.powerConsumption + status.recentSession.powerConsumption))

      _ <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))

  override def stopCharging(status: OutletStatusEvent): Task[ChargerOutlet] =
    (for {
      data <- getByPK(status.outletId).filterOrDie(mayTransitionTo(OutletDeviceState.CablePlugged))(new Error("no data found"))

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

object DynamoDBChargerOutletService {

  val live: ZLayer[DynamoDBExecutor, Nothing, ChargerOutletService] =
    ZLayer.fromFunction(DynamoDBChargerOutletService.apply _)
}
