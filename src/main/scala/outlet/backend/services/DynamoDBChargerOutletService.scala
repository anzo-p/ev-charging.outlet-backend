package outlet.backend.services

import outlet.backend.ChargerOutletService
import outlet.backend.types.ChargerOutlet
import outlet.backend.types.ChargerOutlet.Ops._
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

  private def getByOutletIdAndRfidTag(outletId: UUID, rfidTag: Option[String]): ZIO[DynamoDBExecutor, Throwable, ChargerOutlet] =
    for {
      result <- getByPK(outletId).filterOrFail(r => r.rfidTag == rfidTag.getOrElse(r.rfidTag))(
                 new Error(s"no data found for outletId: $outletId and rfidTag: $rfidTag"))
    } yield result

  private def checkAndSetState(
      outletId: UUID,
      rfidTag: Option[String],
      nextState: OutletDeviceState,
      message: String,
      yieldResult: Boolean = false
    ): ZIO[Any, Throwable, Option[ChargerOutlet]] =
    (for {
      data     <- getByOutletIdAndRfidTag(outletId, rfidTag)
      filtered <- ZIO.succeed(data).filterOrFail(_.mayTransitionTo(nextState))(new Error(message))
      updated  <- ZIO.succeed(filtered.copy(outletState = nextState))
      _        <- putByPK(updated)
    } yield if (yieldResult) Some(updated) else None)
      .provideLayer(ZLayer.succeed(executor))

  private def setOutletStateAggregatesReturningOrFail(event: OutletStatusEvent, targetState: OutletDeviceState): Task[ChargerOutlet] =
    (for {
      _    <- ZIO.succeed(println(s"$event $targetState"))
      data <- getByPK(event.outletId).filterOrFail(_.mayTransitionTo(targetState))(new Error(cannotTransitionTo(targetState)))

      update <- ZIO.succeed(
                 data.copy(
                   outletState      = targetState,
                   startTime        = event.recentSession.periodStart,
                   endTime          = event.recentSession.periodEnd,
                   powerConsumption = data.powerConsumption + event.recentSession.powerConsumption
                 ))

      _ <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))

  override def register(outlet: ChargerOutlet): Task[ChargerOutlet] =
    (for {
      inserted <- ZIO.succeed(outlet)
      _        <- put(tableResource, inserted).execute
    } yield inserted)
      .provideLayer(ZLayer.succeed(executor))

  override def setOutletStateUnit(outletId: UUID, rfidTag: Option[String], targetState: OutletDeviceState): ZIO[Any, Throwable, Unit] =
    checkAndSetState(outletId, rfidTag, targetState, cannotTransitionTo(targetState)).unit

  private def setOutletStateReturningOrFail(
      outletId: UUID,
      rfidTag: String,
      targetState: OutletDeviceState
    ): ZIO[Any, Throwable, ChargerOutlet] =
    checkAndSetState(outletId, Some(rfidTag), targetState, cannotTransitionTo(targetState))
      .flatMap {
        case None =>
          ZIO.fail(new Error("unsuccessful update"))
        case Some(outlet) =>
          ZIO.succeed(outlet)
      }

  override def setChargingRequested(event: OutletStatusEvent): Task[ChargerOutlet] = {
    val targetState = event.outletState
    (for {
      data <- getByPK(event.outletId).filterOrFail(_.mayTransitionTo(targetState))(new Error(cannotTransitionTo(targetState)))

      update <- ZIO.succeed(
                 data.copy(
                   outletState      = targetState,
                   rfidTag          = event.recentSession.rfidTag,
                   startTime        = event.recentSession.periodStart,
                   endTime          = event.recentSession.periodEnd,
                   powerConsumption = data.powerConsumption + event.recentSession.powerConsumption
                 ))

      _ <- putByPK(update)
    } yield update)
      .provideLayer(ZLayer.succeed(executor))
  }

  override def setCharging(outletId: UUID, rfidTag: String): Task[ChargerOutlet] =
    setOutletStateReturningOrFail(outletId, rfidTag, OutletDeviceState.Charging)

  override def aggregateConsumption(event: OutletStatusEvent): Task[ChargerOutlet] =
    setOutletStateAggregatesReturningOrFail(event, OutletDeviceState.Charging)

  override def stopCharging(event: OutletStatusEvent): Task[ChargerOutlet] =
    setOutletStateAggregatesReturningOrFail(event, OutletDeviceState.CablePlugged)
}

object DynamoDBChargerOutletService {

  val live: ZLayer[DynamoDBExecutor, Nothing, ChargerOutletService] =
    ZLayer.fromFunction(DynamoDBChargerOutletService.apply _)
}
