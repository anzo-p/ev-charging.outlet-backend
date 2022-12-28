package outlet.backend.services

import outlet.backend.ChargerOutletService
import outlet.backend.types.chargerOutlet.ChargerOutlet
import shared.db.DynamoDBPrimitives
import shared.types.TimeExtensions.DateTimeSchemaImplicits
import shared.types.enums.OutletDeviceState
import shared.types.outletStatus.OutletStatusEvent
import zio._
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb.ProjectionExpression.$
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

  override def register(outlet: ChargerOutlet): Task[ChargerOutlet] =
    (for {
      inserted <- ZIO.succeed(outlet)
      _        <- put(tableResource, inserted).execute
    } yield inserted)
      .provideLayer(ZLayer.succeed(executor))

  override def setAvailable(outletId: UUID): Task[Unit] =
    (for {
      data <- getByPK(outletId, $("state") in Set(OutletDeviceState.CablePlugged.entryName, OutletDeviceState.Broken.entryName))
      _    <- putByPK(data.copy(state = OutletDeviceState.Available))
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def setCablePlugged(outletId: UUID): Task[Unit] =
    (for {
      data <- getByPK(outletId, $("state") === OutletDeviceState.Available.entryName)
      _    <- putByPK(data.copy(state = OutletDeviceState.CablePlugged))
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def setChargingRequested(outletId: UUID, rfidTag: String): ZIO[Any, Throwable, ChargerOutlet] =
    (for {
      data <- getByPK(
               outletId,
               $("rfidTag") === rfidTag && $("state") === OutletDeviceState.CablePlugged.entryName
             )
      update <- ZIO.succeed(data.copy(state = OutletDeviceState.ChargingRequested))
      _      <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))

  override def beginCharging(outletId: UUID): Task[Unit] =
    (for {
      data <- getByPK(outletId, $("state") === OutletDeviceState.ChargingRequested.entryName)
      _    <- putByPK(data.copy(state = OutletDeviceState.Charging))
    } yield ())
      .provideLayer(ZLayer.succeed(executor))

  override def aggregateConsumption(status: OutletStatusEvent): Task[ChargerOutlet] =
    (for {
      data <- getByPK(
               status.outletId,
               $("rfidTag") === status.recentSession.rfidTag && $("state") === OutletDeviceState.Charging.entryName
             )
      update <- ZIO.succeed(
                 data.copy(
                   endTime          = status.recentSession.periodEnd,
                   powerConsumption = data.powerConsumption + status.recentSession.powerConsumption))
      _ <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))

  override def stopCharging(status: OutletStatusEvent): Task[ChargerOutlet] =
    (for {
      data <- getByPK(
               status.outletId,
               $("rfidTag") === status.recentSession.rfidTag && $("state") === OutletDeviceState.Charging.entryName
             )
      // it doesnt fail if data is "no data found"

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
